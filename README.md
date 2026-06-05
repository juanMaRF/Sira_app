# 🌱 Sira

**Sira** es un sistema de monitoreo de plantas domésticas con una maceta
inteligente. Mide humedad de la tierra, temperatura, luz y nivel de agua, y los
muestra en una app Android en tiempo real.

Este repositorio contiene **las tres partes** del sistema completo: el firmware
de la maceta, el puente en la Raspberry Pi y la app móvil.

---

## Arquitectura

```
┌──────────────┐   MQTT / mTLS   ┌────────────────────┐                ┌──────────────┐
│  ESP32-S3    │ ──────────────► │  Mosquitto         │                │   Firestore  │
│  (maceta)    │  publica el     │  (broker MQTT)     │                │   (Firebase) │
│  4 sensores  │  JSON al tópico │  en la Raspberry   │                │              │
└──────────────┘                 └─────────┬──────────┘                └──────▲───────┘
                                           │                                  │
                                           │  se suscribe al tópico           │ escribe (cuenta
                                           ▼                                  │ de servicio)
                                 ┌────────────────────┐                       │
                                 │  Puente Python     │ ──────────────────────┘
                                 │  (Raspberry Pi)    │   reenvía cada lectura
                                 └────────────────────┘
                                                                              │ lee en tiempo
                                                                              ▼ real / bajo demanda
                                                                       ┌──────────────┐
                                                                       │  App Sira     │
                                                                       │  (Android)    │
                                                                       └──────────────┘
```

**Flujo de una lectura, de punta a punta:**

1. La **ESP32-S3** lee los sensores y publica un JSON por **MQTT con mTLS** en el
   tópico `planta/<CÓDIGO>/sensores`.
2. **Mosquitto** (broker) en la Raspberry Pi recibe el mensaje.
3. El **puente Python** está suscrito a ese tópico; **traduce** el payload (renombra
   campos y convierte unidades) y lo escribe en **Cloud Firestore** con la estructura
   que consume la app.
4. La **app Android** lee Firestore y muestra el estado en el Panel (en vivo) y el
   Histórico (bajo demanda).

---

## Las tres partes

| Parte | Qué es | Tecnología | Guía |
|---|---|---|---|
| [`app/`](app) *(raíz)* | App móvil: login (correo/Google), lista de plantas, dashboard, histórico, detalle por sensor, perfil | Android · Kotlin · Jetpack Compose · MVVM · Firebase | [SETUP_FIREBASE.md](SETUP_FIREBASE.md) |
| [`raspberry-bridge/`](raspberry-bridge) | Puente que traduce los datos de MQTT y los escribe en Firebase | Python · paho-mqtt · firebase-admin | [raspberry-bridge/README.md](raspberry-bridge/README.md) |
| Firmware ESP32-S3 *(proyecto aparte)* | Lee sensores (humedad, nivel, LM35, BH1750), controla válvulas y publica por MQTT/mTLS | C · ESP-IDF · FreeRTOS | — |

> La app Android vive en la **raíz** del repositorio (proyecto de Android
> Studio/Gradle). El puente es una subcarpeta. El **firmware** de la ESP32-S3 es un
> proyecto ESP-IDF aparte; solo debe publicar en `planta/<CÓDIGO>/sensores` (ver el
> contrato en [raspberry-bridge/README.md](raspberry-bridge/README.md)).

---

## La pieza que une todo: el **código de la maceta**

Cada maceta tiene un **código único** (p. ej. `SIRA-7F3A`). Ese mismo código
aparece en los tres lados y es lo que conecta una lectura física con la planta
correcta en la app:

| Dónde | Cómo aparece |
|---|---|
| **ESP32** | `DEVICE_CODE` en el firmware → publica en `planta/SIRA-7F3A/sensores` |
| **Puente** | extrae el código del tópico y escribe en `plants/SIRA-7F3A` |
| **App** | el usuario **registra** la planta con ese código (`plants/SIRA-7F3A`) |

Siempre se maneja en **MAYÚSCULAS** para que el documento coincida en los tres lados.

---

## Modelo de datos en Firestore

```
plants (colección)
  └── SIRA-7F3A   (documento; su ID es el código de la maceta)
        ├── ownerUid:     "abc123"        ← lo pone la APP al registrar/reclamar
        ├── plantName:     "Albahaca"     ← lo pone la APP
        ├── soilMoisture:  62             ┐
        ├── temperature:   23.5           │  los escribe el PUENTE
        ├── lightLevel:    74             │  (datos de la ESP32)
        ├── waterLevel:    88             │
        ├── lastUpdated:   1717000000000  ┘  (epoch millis → activa "En vivo")
        └── history (subcolección)        ← una lectura por documento (Histórico)
```

- La **app** es dueña de `ownerUid` y `plantName` (registro).
- El **puente** escribe solo los campos de sensores con `merge`, sin pisar lo de la app.
- El **histórico** son documentos en `plants/<código>/history`; la app lee los últimos 50.

---

## Puesta en marcha (orden recomendado)

1. **Firebase + App** → sigue [SETUP_FIREBASE.md](SETUP_FIREBASE.md): crear
   proyecto, habilitar Google Sign-In, crear Firestore y reglas de seguridad,
   compilar la app.
2. **Raspberry Pi (puente)** → sigue
   [raspberry-bridge/README.md](raspberry-bridge/README.md): certificados mTLS,
   clave de cuenta de servicio, configurar y dejar corriendo con systemd.
3. **ESP32-S3 (firmware)** → sigue
   [esp32-firmware/README.md](esp32-firmware/README.md): certificado de la ESP32,
   `config.h`, calibración de sensores, compilar y subir con PlatformIO.

> 💡 Puedes probar la **app sola** sin hardware ni nube: en
> `di/ServiceLocator.kt` cambia a las implementaciones `Mock*` y la app funciona
> con datos simulados.

---

## Funciones de la app

- **Inicio de sesión con Google** (Firebase Authentication).
- **Mis plantas**: lista de plantas del usuario, con **registrar** (modelo
  híbrido: reclama o crea), **renombrar** y **eliminar**.
- **Panel**: estado actual de los 4 sensores **en tiempo real**, con indicador
  "En vivo / última actualización".
- **Histórico**: gráfica y lista de lecturas, **bajo demanda** con
  *deslizar para refrescar*.
- **Detalle por sensor**: valor actual, rango ideal, estado y tendencia.

---

## Stack técnico

- **App:** Kotlin, Jetpack Compose, Material 3, arquitectura MVVM, Navigation
  Compose, Firebase Auth + Firestore, Credential Manager (Google Sign-In).
- **Puente:** Python 3, paho-mqtt, firebase-admin (Firestore), systemd.
- **Firmware:** C++ (Arduino/FreeRTOS) sobre PlatformIO, PubSubClient,
  ArduinoJson, WiFiClientSecure (mTLS).
- **Transporte:** MQTT sobre TLS mutuo (mTLS) con Mosquitto.

---

## Notas de seguridad

- Nunca subas al repositorio: `google-services.json`, `serviceAccountKey.json`,
  `config.ini`, `config.h`, ni los certificados/claves (`*.key`, `*.crt`). Cada
  subcarpeta ya tiene su `.gitignore`.
- El **puente** escribe en Firestore con una cuenta de servicio (permisos de
  administrador, salta las reglas). La **app** sí respeta las reglas de seguridad,
  que restringen cada planta a su `ownerUid`.
