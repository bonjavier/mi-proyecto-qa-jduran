"""
Script de DEMO (no es el test oficial) para ver el consumo en vivo.

Uso:
    python consumer_demo.py

Se queda corriendo escuchando el tópico gps-raw-events. Corre esto PRIMERO,
en una terminal, y déjalo abierto. Después, en OTRA terminal, corre
producer_demo.py (una vez, o --count N para un lote aleatorio) y mira
cómo aparecen aquí en tiempo real, con su validación.

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

SEPARATOR = "─" * 60


def connect_with_retry(max_attempts=5):
    """
    En Windows, kafka-python-ng a veces lanza un error de socket
    transitorio ("ValueError: Invalid file descriptor: -1") — no solo al
    conectar, sino en cualquier momento en que la librería tenga que
    reabrir una conexión internamente (heartbeats, rebalances). Por eso
    esta función se usa tanto para la conexión inicial como para
    reconectar si el bucle principal se cae por ese motivo.
    """
    for attempt in range(1, max_attempts + 1):
        try:
            return KafkaConsumer(
                TOPIC_NAME,
                bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
                # "latest" (no "earliest") a propósito: para esta demo en
                # vivo no queremos ver los mensajes viejos acumulados de
                # pruebas anteriores, solo los NUEVOS que mandes desde ahora.
                auto_offset_reset="latest",
                group_id=f"demo-consumer-{uuid.uuid4()}",
                value_deserializer=lambda v: json.loads(v.decode("utf-8")),
            )
        except ValueError as e:
            print(f"[CONSUMER-DEMO] Intento {attempt}/{max_attempts} falló por un error de socket transitorio ({e}); reintentando...")
            time.sleep(1)
    return None


def format_fields(event):
    """Alinea 'clave : valor' para que el JSON se lea como una tabla,
    no como un dict de una sola línea."""
    if not event:
        return "      (mensaje vacío)"
    width = max(len(str(k)) for k in event.keys())
    return "\n".join(f"      {str(k).ljust(width)} : {v}" for k, v in event.items())


def validate_and_print(event, offset, stats):
    print(SEPARATOR)
    print(f"  Mensaje #{stats['total'] + 1}  (offset={offset})")
    print(SEPARATOR)
    print(format_fields(event))
    print()

    stats["total"] += 1

    try:
        validate(instance=event, schema=TELEMETRY_SCHEMA)
    except ValidationError as e:
        print(f"      ✗ Contrato (jsonschema)  RECHAZADO -> {e.message}")
        print()
        stats["rechazados"] += 1
        print(f"  Total: {stats['aceptados']} aceptados · {stats['rechazados']} rechazados\n")
        return

    print("      ✓ Contrato (jsonschema)  OK")

    lat_ok = -90 <= event.get("lat", 0) <= 90
    lng_ok = -180 <= event.get("lng", 0) <= 180
    speed_ok = event.get("speed", -1) >= 0

    if lat_ok and lng_ok and speed_ok:
        print("      ✓ Rangos (lat/lng/speed) OK")
        stats["aceptados"] += 1
    else:
        print(f"      ✗ Rangos (lat/lng/speed) RECHAZADO (lat_ok={lat_ok}, lng_ok={lng_ok}, speed_ok={speed_ok})")
        stats["rechazados"] += 1

    print(f"\n  Total: {stats['aceptados']} aceptados · {stats['rechazados']} rechazados\n")


def main():
    print(f"[CONSUMER-DEMO] Conectando a {KAFKA_BOOTSTRAP_SERVERS}, tópico '{TOPIC_NAME}'...")
    stats = {"total": 0, "aceptados": 0, "rechazados": 0}

    while True:
        consumer = connect_with_retry()
        if consumer is None:
            raise SystemExit("No se pudo conectar el consumer tras varios intentos.")

        print("[CONSUMER-DEMO] Listo. Esperando mensajes nuevos (Ctrl+C para salir)...\n")

        try:
            for message in consumer:
                validate_and_print(message.value, message.offset, stats)
            # El generador termina solo si hay un consumer_timeout_ms
            # configurado (aquí no lo hay), así que en la práctica esto
            # no debería alcanzarse — se queda escuchando para siempre.
            break
        except ValueError as e:
            # Mismo error transitorio de socket, pero esta vez ocurrió
            # DURANTE la escucha (no al conectar). Reconectamos con un
            # consumer nuevo y seguimos — es una demo interactiva, no
            # pasa nada por perder de vista un mensaje que llegó justo
            # en el instante del corte.
            print(f"\n[CONSUMER-DEMO] Error de socket transitorio durante la escucha ({e}); reconectando...\n")
            try:
                consumer.close()
            except Exception:
                pass
            time.sleep(1)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n[CONSUMER-DEMO] Cerrado por el usuario (Ctrl+C).")
