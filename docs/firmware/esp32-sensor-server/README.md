# ESP32 GestureMacro Sensor Server

Reference Arduino/ESP32 firmware implementing the `gesturemacro/1` wire protocol
over both BLE GATT and WebSocket. See `docs/protocol/remote-sensor-v1.md` for the
full protocol specification.

## Hardware

Any ESP32 development board (ESP32-WROOM, ESP32-S3, etc.) with:
- DHT22 or SHT31 for temperature + humidity
- MH-Z19 or SCD30 for CO2 (optional)
- Standard Arduino IDE or PlatformIO

## Libraries

```ini
; platformio.ini
[env:esp32dev]
platform = espressif32
board = esp32dev
framework = arduino
lib_deps =
  h2zero/NimBLE-Arduino @ ^1.4.2      ; BLE GATT
  links2004/arduinoWebSockets @ ^2.4   ; WebSocket server
  adafruit/DHT sensor library @ ^1.4   ; Temperature/humidity
```

## Quick start

1. Copy `esp32_sensor_server.ino` into the Arduino IDE
2. Set `WIFI_SSID` and `WIFI_PASS` in the sketch
3. Flash to ESP32
4. In GestureMacro → Sensor devices → Scan (BLE) **or** Connect via IP (WebSocket)

## Protocol endpoints

### BLE GATT
- Advertises service UUID `4e475552-4f4d-4143-524f-000000000001`
- Characteristic `...0002` (READ+NOTIFY): capability handshake JSON
- Characteristic `...0003` (NOTIFY): streaming samples at configured Hz
- Characteristic `...0004` (READ+WRITE): `0x00` JSON / `0x01` MessagePack

### WebSocket
- Listens on port 8765 (configurable)
- On connect: sends capability handshake as first text frame
- Streams samples as text (JSON) or binary (MessagePack) frames
- Sends WebSocket ping every 30 s

## Sketch reference

See `esp32_sensor_server.ino` — full reference implementation tracked in ticket-061.
