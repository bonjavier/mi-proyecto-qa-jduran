"""
Script de DEMO (no es el test oficial) para ver el consumo en vivo.

Uso:
    python consumer_demo.py

Se queda corriendo escuchando el tópico gps-raw-events. Corre esto PRIMERO,
en una terminal, y déjalo abierto. Después, en OTRA terminal, corre
producer_demo.py (una o varias veces, con distintos mensajes) y mira cómo
aparecen aquí en tiempo real, con su validación.

Para salir: Ctrl+C.
"""

import json
import time
import uuid

from jsonschema import validate, ValidationError
from kafka import KafkaConsumer

from schemas.telemetry_schema import TELEMETRY_SCHEMA

KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
TOPIC_NAME = "gps-raw-events"

print(f"[CONSUMER-DEMO] Conectando a {KAFKA_BOOTSTRAP_SERVERS}, tópico '{TOPIC_NAME}'...")

# Ver test_gps_events.py para la explicación completa: en Windows, la
# primera conexión a veces falla con un error de socket transitorio de
# la librería (no relacionado con Kafka en sí). Reintentamos unas pocas
# veces si pasa.
consumer = None
for attempt in range(1, 6):
    try:
        consumer = KafkaConsumer(
            TOPIC_NAME,
            bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
            # "latest" (no "earliest") a propósito aquí: para esta demo en
            # vivo NO queremos ver los mensajes viejos acumulados de
            # pruebas anteriores, solo los mensajes NUEVOS que mandes
            # desde ahora.
            auto_offset_reset="latest",
            group_id=f"demo-consumer-{uuid.uuid4()}",
            value_deserializer=lambda v: json.loads(v.decode("utf-8")),
        )
        break
    except ValueError as e:
        print(f"[CONSUMER-DEMO] Intento {attempt}/5 falló por un error de socket transitorio ({e}); reintentando...")
        time.sleep(1)

if consumer is None:
    raise SystemExit("No se pudo conectar el consumer tras varios intentos.")

print("[CONSUMER-DEMO] Listo. Esperando mensajes nuevos (Ctrl+C para salir)...\n")

for message in consumer:
    event = message.value
    print(f"--- Mensaje recibido (offset={message.offset}) ---")
    print(f"  Contenido: {event}")

    try:
        validate(instance=event, schema=TELEMETRY_SCHEMA)
        print("  Validación de contrato (jsonschema): OK")
    except ValidationError as e:
        print(f"  Validación de contrato (jsonschema): RECHAZADO -> {e.message}")
        continue

    lat_ok = -90 <= event.get("lat", 0) <= 90
    lng_ok = -180 <= event.get("lng", 0) <= 180
    speed_ok = event.get("speed", -1) >= 0
    if lat_ok and lng_ok and speed_ok:
        print("  Validación de rangos: OK")
    else:
        print(f"  Validación de rangos: RECHAZADO (lat_ok={lat_ok}, lng_ok={lng_ok}, speed_ok={speed_ok})")

    print()
