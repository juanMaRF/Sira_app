#!/usr/bin/env python3
"""
Puente MQTT -> Firebase para Sira.

Se suscribe al tópico donde la ESP32-S3 publica las lecturas (a través de
Mosquitto con mTLS), TRADUCE el payload del firmware al esquema que consume la
app Android y lo escribe en Cloud Firestore:

  plants/{codigo}                      <- estado actual (Panel)
  plants/{codigo}/history/{auto}       <- una lectura por documento (Histórico)

--- Contrato con el firmware (ESP-IDF) ---
Tópico:  planta/<CODIGO>/sensores         (el <CODIGO> = ID de la maceta)
Payload: {"hum_mv":..,"nivel_pct":..,"temp_c":..,"lux":..,"valvulas":..., ...}

El puente convierte:
  temp_c    -> temperature (°C, directo)
  nivel_pct -> waterLevel  (%, directo)
  hum_mv    -> soilMoisture (% calculado a partir de los mV)
  lux       -> lightLevel  (% en escala logarítmica)   [si lux es null, se omite]
Los campos crudos (lux, valvulas) también se guardan por si se muestran luego.
"""

import configparser
import json
import logging
import math
import os
import ssl
import sys
import time

import paho.mqtt.client as mqtt
import firebase_admin
from firebase_admin import credentials, firestore

CONFIG_PATH = os.path.join(os.path.dirname(os.path.abspath(__file__)), "config.ini")

logging.basicConfig(level=logging.INFO, format="%(asctime)s [%(levelname)s] %(message)s")
log = logging.getLogger("sira-bridge")


# ---------------------------------------------------------------------------
# Configuración
# ---------------------------------------------------------------------------
def load_config() -> configparser.ConfigParser:
    if not os.path.exists(CONFIG_PATH):
        log.error("No existe config.ini. Copia config.example.ini a config.ini y edítalo.")
        sys.exit(1)
    cfg = configparser.ConfigParser()
    cfg.read(CONFIG_PATH)
    return cfg


def load_mapping(cfg: configparser.ConfigParser) -> dict:
    """Lee la sección [payload]: nombres de campos del firmware y calibración."""
    p = "payload"
    return {
        "f_temp": cfg.get(p, "field_temperature", fallback="temp_c"),
        "f_water": cfg.get(p, "field_water_pct", fallback="nivel_pct"),
        "f_soil_mv": cfg.get(p, "field_soil_mv", fallback="hum_mv"),
        "f_lux": cfg.get(p, "field_light_lux", fallback="lux"),
        "f_valves": cfg.get(p, "field_valves", fallback="valvulas"),
        "soil_dry_mv": cfg.getfloat(p, "soil_dry_mv", fallback=300.0),
        "soil_wet_mv": cfg.getfloat(p, "soil_wet_mv", fallback=700.0),
        "light_full_lux": cfg.getfloat(p, "light_full_lux", fallback=10000.0),
    }


def init_firestore(cfg: configparser.ConfigParser):
    sa_path = cfg.get("firebase", "service_account")
    if not os.path.exists(sa_path):
        log.error("No se encontró la clave de servicio de Firebase: %s", sa_path)
        sys.exit(1)
    cred = credentials.Certificate(sa_path)
    firebase_admin.initialize_app(cred)
    log.info("Firebase inicializado.")
    return firestore.client()


# ---------------------------------------------------------------------------
# Conversión de unidades
# ---------------------------------------------------------------------------
def soil_mv_to_percent(mv: float, dry_mv: float, wet_mv: float) -> int:
    """mV del sensor de humedad -> 0..100 % (mayor mV = más húmedo)."""
    if wet_mv == dry_mv:
        return 0
    pct = (mv - dry_mv) * 100.0 / (wet_mv - dry_mv)
    return int(max(0, min(100, round(pct))))


def lux_to_percent(lux: float, full_lux: float):
    """lux -> 0..100 % en escala logarítmica (full_lux ~ 100%)."""
    if lux is None or lux < 0:
        return None
    if lux <= 1:
        return 0
    pct = math.log10(lux) / math.log10(full_lux) * 100.0
    return int(max(0, min(100, round(pct))))


def extract_device_id(cfg, topic: str) -> str:
    """Código de la maceta tomado del tópico (planta/<CODIGO>/sensores), en MAYÚSCULAS."""
    idx = cfg.getint("bridge", "device_id_topic_index", fallback=1)
    parts = topic.split("/")
    if idx >= len(parts):
        return ""
    return parts[idx].strip().upper()


def translate(payload: dict, m: dict) -> dict:
    """Convierte el payload del firmware al esquema de la app."""
    reading = {
        "temperature": float(payload[m["f_temp"]]),
        "waterLevel": int(round(float(payload[m["f_water"]]))),
        "soilMoisture": soil_mv_to_percent(
            float(payload[m["f_soil_mv"]]), m["soil_dry_mv"], m["soil_wet_mv"]
        ),
        "lastUpdated": int(time.time() * 1000),  # epoch millis (lo lee la app)
    }
    lux = payload.get(m["f_lux"])
    light_pct = lux_to_percent(lux, m["light_full_lux"]) if lux is not None else None
    if light_pct is not None:
        reading["lightLevel"] = light_pct
        reading["lux"] = float(lux)  # valor crudo, por si se muestra luego
    valves = payload.get(m["f_valves"])
    if valves is not None:
        reading["valves"] = bool(valves)
    return reading


# ---------------------------------------------------------------------------
# Callbacks MQTT (API v2 de paho-mqtt >= 2.0)
# ---------------------------------------------------------------------------
def on_connect(client, userdata, flags, reason_code, properties):
    if reason_code == 0:
        topic = userdata["topic"]
        client.subscribe(topic)
        log.info("Conectado al broker. Suscrito a '%s'.", topic)
    else:
        log.error("Fallo de conexión al broker (código %s).", reason_code)


def on_message(client, userdata, msg):
    cfg = userdata["cfg"]
    mapping = userdata["mapping"]
    db = userdata["db"]
    collection = userdata["collection"]

    try:
        payload = json.loads(msg.payload.decode("utf-8"))
    except (ValueError, UnicodeDecodeError) as exc:
        log.warning("Payload no es JSON válido en '%s': %s", msg.topic, exc)
        return
    if not isinstance(payload, dict):
        log.warning("Payload no es un objeto JSON en '%s'.", msg.topic)
        return

    device_id = extract_device_id(cfg, msg.topic)
    if not device_id:
        log.warning("No se pudo determinar el código de la maceta (tópico '%s').", msg.topic)
        return

    required = (mapping["f_temp"], mapping["f_water"], mapping["f_soil_mv"])
    missing = [f for f in required if f not in payload]
    if missing:
        log.warning("Faltan campos %s para '%s'.", missing, device_id)
        return

    try:
        reading = translate(payload, mapping)
    except (TypeError, ValueError, KeyError) as exc:
        log.warning("Valores inválidos para '%s': %s", device_id, exc)
        return

    try:
        plant_ref = db.collection(collection).document(device_id)
        # merge=True actualiza solo los sensores; conserva ownerUid y plantName.
        plant_ref.set(reading, merge=True)
        plant_ref.collection("history").add(reading)
        log.info("Lectura guardada para '%s': %s", device_id, reading)
    except Exception as exc:  # noqa: BLE001 - registrar y seguir vivo
        log.error("Error escribiendo en Firestore para '%s': %s", device_id, exc)


def build_mqtt_client(cfg, userdata) -> mqtt.Client:
    client = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, userdata=userdata)
    client.on_connect = on_connect
    client.on_message = on_message

    ca = cfg.get("mqtt", "ca_cert", fallback="").strip()
    cert = cfg.get("mqtt", "client_cert", fallback="").strip()
    key = cfg.get("mqtt", "client_key", fallback="").strip()
    if ca:
        client.tls_set(
            ca_certs=ca,
            certfile=cert or None,
            keyfile=key or None,
            tls_version=ssl.PROTOCOL_TLS_CLIENT,
        )
        if cfg.getboolean("mqtt", "tls_insecure", fallback=False):
            client.tls_insecure_set(True)
    return client


def main():
    cfg = load_config()
    mapping = load_mapping(cfg)
    db = init_firestore(cfg)

    host = cfg.get("mqtt", "host", fallback="localhost")
    port = cfg.getint("mqtt", "port", fallback=8883)
    topic = cfg.get("mqtt", "topic", fallback="planta/+/sensores")
    collection = cfg.get("firebase", "collection", fallback="plants")

    userdata = {
        "cfg": cfg, "mapping": mapping, "db": db,
        "collection": collection, "topic": topic,
    }
    client = build_mqtt_client(cfg, userdata)

    log.info("Conectando a %s:%s ...", host, port)
    client.connect(host, port, keepalive=60)
    client.loop_forever()  # bloquea y reconecta automáticamente


if __name__ == "__main__":
    main()
