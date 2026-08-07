"""
Reporte de calidad de datos + consumer lag para el bonus de Kafka.

No es el test oficial (ver test_gps_events.py) — es una herramienta de QA
para tomar decisiones, no solo para pasar/fallar: genera un lote de
mensajes, los valida todos, y resume:

  1. Tasa de calidad de datos: % de mensajes válidos vs. inválidos.
  2. Desglose de motivos de rechazo (para priorizar qué arreglar aguas
     arriba: firmware del GPS, la app del conductor, un sensor, etc.).
  3. Consumer lag: cuántos mensajes le faltan al consumidor por leer del
     tópico — la métrica operativa #1 de Kafka. En una plataforma de
     telemetría de flotas, lag alto significa perder visibilidad en
     tiempo real de dónde están los vehículos: un riesgo de negocio,
     no solo técnico.

Uso:
    python quality_report.py --count 100
    python quality_report.py --count 100 --seed 42   # reproducible
"""

import argparse
import json
import random
import time
import uuid

from jsonschema import validate, ValidationError
from kafka import KafkaProducer, KafkaConsumer, KafkaAdminClient, TopicPartition
from kafka.admin import NewTopic
from kafka.errors import TopicAlreadyExistsError

from schemas.telemetry_schema import TELEMETRY_SCHEMA
from producer_demo import CASE_GENERATORS, TOPIC_NAME, KAFKA_BOOTSTRAP_SERVERS

SEPARATOR = "─" * 60


def ensure_topic_exists():
    admin = KafkaAdminClient(bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS)
    try:
        admin.create_topics([NewTopic(name=TOPIC_NAME, num_partitions=1, replication_factor=1)])
    except TopicAlreadyExistsError:
        pass
    finally:
        admin.close()


def classify_rejection(error: ValidationError) -> str:
    """Agrupa el error de jsonschema en una categoría legible para el
    reporte, en vez de mostrar el mensaje técnico crudo."""
    if error.validator == "required":
        return "campo_faltante"
    if error.validator == "pattern":
        return "formato_vehicleId_invalido"
    if error.validator in ("minimum", "maximum"):
        return "fuera_de_rango"
    if error.validator == "type":
        return "tipo_incorrecto"
    return "otro"


def connect_consumer_with_retry(group_id, max_attempts=5):
    for attempt in range(1, max_attempts + 1):
        try:
            return KafkaConsumer(
                TOPIC_NAME,
                bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
                auto_offset_reset="earliest",
                group_id=group_id,
                value_deserializer=lambda v: json.loads(v.decode("utf-8")),
                consumer_timeout_ms=10000,
            )
        except ValueError as e:
            print(f"[REPORT] Intento {attempt}/{max_attempts} falló por un error de socket transitorio ({e}); reintentando...")
            time.sleep(1)
    raise SystemExit("No se pudo conectar el consumer tras varios intentos.")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--count", type=int, default=50, help="Cantidad de mensajes a generar y analizar (default 50).")
    parser.add_argument("--seed", type=int, default=None, help="Semilla para reproducibilidad (opcional).")
    args = parser.parse_args()

    if args.seed is not None:
        random.seed(args.seed)

    ensure_topic_exists()

    # ── Generar y publicar el lote ──────────────────────────────────
    print(f"[REPORT] Generando y publicando {args.count} mensajes...")
    producer = KafkaProducer(
        bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
        value_serializer=lambda v: json.dumps(v).encode("utf-8"),
    )
    case_names = list(CASE_GENERATORS.keys())
    sent_offsets = []
    partition_used = None
    for _ in range(args.count):
        case_name = random.choice(case_names)
        event = CASE_GENERATORS[case_name]()
        future = producer.send(TOPIC_NAME, value=event)
        record_metadata = future.get(timeout=10)
        sent_offsets.append(record_metadata.offset)
        partition_used = record_metadata.partition
    producer.flush()
    producer.close()
    print(f"[REPORT] Publicados. Offsets {min(sent_offsets)}..{max(sent_offsets)} en partición {partition_used}.\n")

    # ── Consumir y validar cada uno ─────────────────────────────────
    group_id = f"quality-report-{uuid.uuid4()}"
    consumer = connect_consumer_with_retry(group_id)

    total = 0
    aceptados = 0
    rechazo_por_categoria = {}
    pending_offsets = set(sent_offsets)

    for message in consumer:
        if message.offset not in pending_offsets:
            continue
        pending_offsets.discard(message.offset)
        total += 1

        try:
            validate(instance=message.value, schema=TELEMETRY_SCHEMA)
            aceptados += 1
        except ValidationError as e:
            categoria = classify_rejection(e)
            rechazo_por_categoria[categoria] = rechazo_por_categoria.get(categoria, 0) + 1

        if not pending_offsets:
            break

    # ── Consumer lag: cuánto le falta al consumidor por leer ───────
    # Se usa la posición REAL que Kafka lleva internamente para este
    # consumer (consumer.position), no un conteo manual: así el lag
    # refleja exactamente lo que Kafka considera "pendiente", incluyendo
    # cualquier mensaje que haya entrado al tópico mientras corría este
    # reporte (por ejemplo, otra terminal con producer_demo.py abierta).
    tp = TopicPartition(TOPIC_NAME, partition_used)
    end_offsets = consumer.end_offsets([tp])
    highwater = end_offsets[tp]
    position = consumer.position(tp)
    lag = max(highwater - position, 0)
    consumer.close()

    # ── Reporte ──────────────────────────────────────────────────────
    rechazados = total - aceptados
    tasa_calidad = (aceptados / total * 100) if total else 0

    print(SEPARATOR)
    print("  REPORTE DE CALIDAD DE DATOS")
    print(SEPARATOR)
    print(f"      Mensajes analizados : {total}")
    print(f"      Válidos             : {aceptados}  ({tasa_calidad:.1f}%)")
    print(f"      Inválidos           : {rechazados}  ({100 - tasa_calidad:.1f}%)")

    if rechazo_por_categoria:
        print("\n      Desglose de motivos de rechazo:")
        cat_width = max(len(c) for c in rechazo_por_categoria)
        for categoria, n in sorted(rechazo_por_categoria.items(), key=lambda kv: -kv[1]):
            pct = n / rechazados * 100 if rechazados else 0
            barra = "█" * n
            print(f"        {categoria.ljust(cat_width)} : {str(n).rjust(3)}  ({pct:4.1f}%)  {barra}")

    print(f"\n      Consumer lag (mensajes sin leer en el tópico): {lag}")
    if lag == 0:
        print("      -> El consumidor está al día. Sin riesgo de pérdida de visibilidad.")
    else:
        print("      -> El consumidor va atrasado. Riesgo: eventos de telemetría acumulándose sin procesar.")
        print("      -> (Si esto corrió mientras otra terminal seguía publicando mensajes,")
        print("         ese tráfico externo también cuenta aquí — es exactamente lo que")
        print("         esta métrica está pensada para detectar.)")
    print(SEPARATOR)


if __name__ == "__main__":
    main()
