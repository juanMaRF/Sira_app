# Puente MQTT → Firebase (Raspberry Pi 4)

Corre en la **Raspberry Pi 4**. Escucha las lecturas que la **ESP32-S3** publica en
**Mosquitto (MQTT con mTLS)**, **traduce** el payload del firmware al esquema que
consume la app **Sira** y lo escribe en **Cloud Firestore**.

```
ESP32-S3 ──MQTT/mTLS──► Mosquitto ──► [puente: traduce campos+unidades] ──► Firestore ──► App
 planta/<CODIGO>/sensores
```

---

## Contrato con el firmware

**Tópico:** `planta/<CODIGO>/sensores` — el `<CODIGO>` es el código de la maceta
(el mismo que registras en la app, en MAYÚSCULAS). En el firmware (ESP-IDF):

```c
#define DEVICE_CODE "SIRA-7F3A"
#define MQTT_TOPIC  "planta/" DEVICE_CODE "/sensores"
```

**Payload (JSON)** que envía el firmware, p. ej.:

```json
{
  "hum_mv": 520, "hum_estado": "suelo humedo",
  "nivel_mv": 1800, "nivel_pct": 56,
  "temp_c": 23.5, "temp_estado": "temperatura ambiente",
  "lux": 345.6, "lux_estado": "moderado - luz indirecta",
  "valvulas": true
}
```

`lux` puede llegar como `null` si el BH1750 falla; el puente lo maneja.

---

## Cómo traduce el puente (firmware → app)

| Campo de la app | Origen en el firmware | Conversión |
|---|---|---|
| `temperature` (°C) | `temp_c` | directo |
| `waterLevel` (%) | `nivel_pct` | directo |
| `soilMoisture` (%) | `hum_mv` | mV → % (lineal entre `soil_dry_mv` y `soil_wet_mv`) |
| `lightLevel` (%) | `lux` | lux → % (escala **logarítmica**, `light_full_lux` ≈ 100%) |

Además guarda los crudos `lux` y `valvulas` por si los quieres mostrar luego.
Todas las constantes de conversión están en la sección `[payload]` de `config.ini`.

> **Sobre las conversiones:** pasar `hum_mv`→% y `lux`→% es aproximado (tu sensor
> de luz mide lux reales). Ajusta `soil_dry_mv`/`soil_wet_mv` con tus umbrales y
> `light_full_lux` según el entorno de tu planta.

---

## 0. Requisitos

- Raspberry Pi 4 con **Raspberry Pi OS** y terminal (SSH o monitor).
- **Mosquitto con mTLS** ya funcionando (tú ya lo tienes). Necesitas el `ca.crt`
  y poder firmar un certificado de cliente para el puente.
- Un proyecto de **Firebase** con Firestore (el mismo de la app).
- Python 3.9+ (`python3 --version`).

---

## 1. Copiar el código a la Raspberry

Copia esta carpeta a la Pi, p. ej. a `/home/pi/sira-bridge`:

```bash
scp -r raspberry-bridge pi@192.168.1.50:/home/pi/sira-bridge
```

---

## 2. Certificado de cliente para el puente (mTLS)

El puente es otro cliente MQTT; necesita su certificado **firmado por tu CA**.
Donde tengas `ca.crt` y `ca.key`:

```bash
openssl genrsa -out bridge.key 2048
openssl req -new -key bridge.key -out bridge.csr -subj "/CN=sira-bridge"
openssl x509 -req -in bridge.csr -CA ca.crt -CAkey ca.key \
  -CAcreateserial -out bridge.crt -days 3650 -sha256

sudo cp ca.crt bridge.crt bridge.key /etc/mosquitto/certs/
sudo chmod 600 /etc/mosquitto/certs/bridge.key
```

---

## 3. Clave de cuenta de servicio de Firebase

1. Consola de Firebase → ⚙️ **Configuración del proyecto** → **Cuentas de servicio**.
2. **Generar nueva clave privada** → descarga el `.json`.
3. Cópialo a la Pi como `serviceAccountKey.json` en `/home/pi/sira-bridge` y protégelo:
   ```bash
   chmod 600 /home/pi/sira-bridge/serviceAccountKey.json
   ```

> ⚠️ Esta clave da acceso de administrador a Firebase. No la subas a ningún repositorio.

---

## 4. Instalar dependencias

```bash
cd /home/pi/sira-bridge
python3 -m venv .venv
source .venv/bin/activate
pip install --upgrade pip
pip install -r requirements.txt
```

---

## 5. Configurar

```bash
cp config.example.ini config.ini
nano config.ini
```

Revisa: `[mqtt]` host/puerto/tópico y rutas de certificados, `[firebase]`
service_account, y `[payload]` (nombres de campos y calibración de las conversiones).

---

## 6. Probar manualmente

```bash
source .venv/bin/activate
python mqtt_firebase_bridge.py
```

Publica un mensaje de prueba imitando al firmware:

```bash
mosquitto_pub -h 192.168.1.50 -p 8883 \
  --cafile /etc/mosquitto/certs/ca.crt \
  --cert /etc/mosquitto/certs/bridge.crt \
  --key /etc/mosquitto/certs/bridge.key \
  -t "planta/SIRA-7F3A/sensores" \
  -m '{"hum_mv":520,"nivel_pct":56,"temp_c":23.5,"lux":345.6,"valvulas":true}'
```

En el log verás `Lectura guardada para 'SIRA-7F3A': {...}` ya con los campos
traducidos (`soilMoisture`, `temperature`, `lightLevel`, `waterLevel`). Si en la
app registras la planta con el código `SIRA-7F3A`, verás los datos.

---

## 7. Dejarlo corriendo siempre (systemd)

```bash
sudo cp /home/pi/sira-bridge/sira-bridge.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable sira-bridge.service
sudo systemctl start sira-bridge.service
journalctl -u sira-bridge.service -f   # logs en vivo
```

---

## 8. Solución de problemas

| Síntoma | Causa probable / solución |
|---|---|
| `Fallo de conexión al broker` | Host/puerto mal, broker apagado, o falta el certificado de cliente. |
| Error de verificación TLS / hostname | `host` no coincide con el CN/SAN del cert del broker. Usa el nombre correcto o `tls_insecure = true` para probar. |
| Conecta pero no guarda nada | El tópico no coincide con el del firmware, o faltan campos. Revisa el log. |
| `Faltan campos [...]` | El firmware no envió `temp_c`, `nivel_pct` o `hum_mv`. |
| Guarda pero la planta no aparece en la app | El `<CODIGO>` del tópico no coincide con el código registrado, o aún no la registraste en la app. |
| Humedad/luz con valores raros | Ajusta `soil_dry_mv`/`soil_wet_mv` y `light_full_lux` en `[payload]`. |

---

## 9. Notas

- Protege `serviceAccountKey.json`, `bridge.key` y `config.ini` (`.gitignore` ya los excluye).
- El histórico crece sin límite; la app lee las últimas 50. Si quieres, programa una
  limpieza periódica.
- El puente escribe con la cuenta de servicio (permisos de admin), por eso no
  depende de las reglas de seguridad de la app.
