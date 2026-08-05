"""
Script de DEMO (no es el test oficial) para publicar mensajes a mano.

Uso — mensaje de ejemplo por defecto:
    python producer_demo.py

Uso — tu propio JSON (para probar distintas estructuras/valores):
    python producer_demo.py --json "{\"vehicleId\": \"VEH-01\", \"lat\": 10.5, \"lng\": -73.2, \"speed\": 40}"

Ejemplos para probar (cópialos y pégalos tal cual, con las comillas):

  Válido, otro vehículo:
    python producer_demo.py --json "{\"vehicleId\": \"VEH-01\", \"lat\": -33.45, \"lng\": -70.66, \"speed\": 0}"

  INVÁLIDO -> lat fuera de rango (debe ser -90..90):
    python producer_demo.py --json "{\"vehicleId\": \"VEH-02\", \"lat\": 200, \"lng\": -70.66, \"speed\": 40}"

  INVÁLIDO -> speed negativo:
    python producer_demo.py --json "{\"vehicleId\": \"VEH-03\", \"lat\": 10, \"lng\": 10, \"speed\": -5}"

  INVÁLIDO -> falta un campo requerido (lng):
    python producer_demo.py --json "{\"vehicleId\": \"VEH-04\", \"lat\": 10, \"speed\": 40}"

  INVÁLIDO -> tipo incorrecto (speed como texto, no número):
    python producer_demo.py --json "{\"vehicleId\": \"VEH-05\", \"lat\": 10, \"lng\": 10, \"speed\": \"rapido\"}"

El PRODUCTOR NO valida nada — Kafka no sabe ni le importa si el JSON es
"correcto" según nuestras reglas de negocio, solo lo transporta como bytes.
La validación pasa del lado del CONSUMIDOR (consumer_demo.py). Por eso
incluso los ejemplos "inválidos" de arriba se van a publicar sin problema
-- lo interesante es ver cómo el consumer, del otro lado, SÍ los rechaza.
"""

import argparse
import json

from kafka import KafkaProducer

KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
TOPIC_NAME = "gps-raw-events"

DEFAULT_EVENT = {
    "vehicleId": "VEH-99",
    "lat": 4.60,
    "lng": -74.08,
    "speed": 65,
}


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--json",
        dest="json_payload",
        default=None,
        help="JSON a enviar. Si no se pasa, usa el evento de ejemplo del reto.",
    )
    args = parser.parse_args()

    if args.json_payload:
        event = json.loads(args.json_payload)
    else:
        event = DEFAULT_EVENT

    print(f"[PRODUCER-DEMO] Conectando a {KAFKA_BOOTSTRAP_SERVERS}...")
    producer = KafkaProducer(
        bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
        value_serializer=lambda v: json.dumps(v).encode("utf-8"),
    )

    print(f"[PRODUCER-DEMO] Enviando a '{TOPIC_NAME}': {event}")
    future = producer.send(TOPIC_NAME, value=event)
    producer.flush()
    record_metadata = future.get(timeout=10)
    print(
        f"[PRODUCER-DEMO] Confirmado por el broker: partición={record_metadata.partition} "
        f"offset={record_metadata.offset}"
    )
    producer.close()


if __name__ == "__main__":
    main()
