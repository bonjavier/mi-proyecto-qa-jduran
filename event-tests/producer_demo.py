"""
Script de DEMO (no es el test oficial) para publicar mensajes a mano o
en lote, generados al azar, mezclando casos válidos, inválidos y borde.

Uso — un mensaje de ejemplo (el del reto):
    python producer_demo.py

Uso — tu propio JSON (vehicleId debe ser 3 letras + guion + 3 dígitos):
    python producer_demo.py --json '{"vehicleId": "VEH-001", "lat": 10.5, "lng": -73.2, "speed": 40}'

Uso — N mensajes generados al azar (mezcla de casos):
    python producer_demo.py --count 20
    python producer_demo.py --count 20 --seed 42   # reproducible: mismos "aleatorios" cada vez

Con --count, cada mensaje se elige al azar entre estos tipos de caso:
  - valido                    -> debería ser ACEPTADO por el consumer
  - lat_fuera_de_rango         -> RECHAZADO (lat fuera de -90..90)
  - lng_fuera_de_rango         -> RECHAZADO (lng fuera de -180..180)
  - speed_negativo             -> RECHAZADO (speed < 0)
  - campo_faltante             -> RECHAZADO (falta vehicleId/lat/lng/speed)
  - tipo_incorrecto            -> RECHAZADO (un campo numérico llega como texto)
  - vehicleId_formato_invalido  -> RECHAZADO (no cumple "AAA-999": minúsculas, largo distinto, sin guion...)
  - borde_valido                -> ACEPTADO (lat/lng/speed exactamente en el límite: -90, 90, -180, 180, 0)

El PRODUCTOR NO valida nada — Kafka no sabe ni le importa si el JSON es
"correcto" según nuestras reglas de negocio, solo lo transporta como bytes.
La validación pasa del lado del CONSUMIDOR (consumer_demo.py). Por eso
incluso los casos "inválidos" se publican sin problema — lo interesante
es ver cómo el consumer, del otro lado, SÍ los rechaza (y por qué).
"""

import argparse
import json
import random
import time

from kafka import KafkaProducer

KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
TOPIC_NAME = "gps-raw-events"

SEPARATOR = "─" * 60

DEFAULT_EVENT = {
    "vehicleId": "VEH-099",
    "lat": 4.60,
    "lng": -74.08,
    "speed": 65,
}


def _random_vehicle_id():
    # Formato del contrato: exactamente 3 letras mayúsculas + guion +
    # 3 dígitos (ver schemas/telemetry_schema.py). Letras al azar (no
    # siempre "VEH") para tener variedad real de flotas/tipos de vehículo.
    letras = "".join(random.choices("ABCDEFGHIJKLMNOPQRSTUVWXYZ", k=3))
    numeros = random.randint(0, 999)
    return f"{letras}-{numeros:03d}"


def case_valido():
    return {
        "vehicleId": _random_vehicle_id(),
        "lat": round(random.uniform(-90, 90), 4),
        "lng": round(random.uniform(-180, 180), 4),
        "speed": round(random.uniform(0, 150), 1),
    }


def case_lat_fuera_de_rango():
    event = case_valido()
    # Fuera de rango hacia arriba o hacia abajo, al azar.
    event["lat"] = random.choice([
        round(random.uniform(90.01, 300), 2),
        round(random.uniform(-300, -90.01), 2),
    ])
    return event


def case_lng_fuera_de_rango():
    event = case_valido()
    event["lng"] = random.choice([
        round(random.uniform(180.01, 400), 2),
        round(random.uniform(-400, -180.01), 2),
    ])
    return event


def case_speed_negativo():
    event = case_valido()
    event["speed"] = round(random.uniform(-100, -0.1), 1)
    return event


def case_campo_faltante():
    event = case_valido()
    campo = random.choice(["vehicleId", "lat", "lng", "speed"])
    del event[campo]
    return event


def case_tipo_incorrecto():
    event = case_valido()
    campo = random.choice(["lat", "lng", "speed"])
    event[campo] = random.choice(["no-es-numero", "rapido", "N/A"])
    return event


def case_vehicleId_formato_invalido():
    event = case_valido()
    variante = random.choice([
        "minusculas", "sin_guion", "pocos_digitos", "muchos_digitos",
        "pocas_letras", "muchas_letras", "letras_donde_van_numeros",
    ])
    if variante == "minusculas":
        event["vehicleId"] = event["vehicleId"].lower()
    elif variante == "sin_guion":
        event["vehicleId"] = event["vehicleId"].replace("-", "")
    elif variante == "pocos_digitos":
        letras = event["vehicleId"].split("-")[0]
        event["vehicleId"] = f"{letras}-{random.randint(0, 99):02d}"
    elif variante == "muchos_digitos":
        letras = event["vehicleId"].split("-")[0]
        event["vehicleId"] = f"{letras}-{random.randint(0, 9999):04d}"
    elif variante == "pocas_letras":
        numeros = event["vehicleId"].split("-")[1]
        event["vehicleId"] = f"AB-{numeros}"
    elif variante == "muchas_letras":
        numeros = event["vehicleId"].split("-")[1]
        event["vehicleId"] = f"ABCD-{numeros}"
    elif variante == "letras_donde_van_numeros":
        letras = event["vehicleId"].split("-")[0]
        event["vehicleId"] = f"{letras}-XYZ"
    return event


def case_borde_valido():
    # Valores EXACTAMENTE en el límite permitido: jsonschema usa
    # minimum/maximum inclusivos, así que esto debe ser ACEPTADO.
    # Sirve para probar que el schema no rechaza por error el borde.
    return {
        "vehicleId": _random_vehicle_id(),
        "lat": random.choice([-90, 90]),
        "lng": random.choice([-180, 180]),
        "speed": 0,
    }


CASE_GENERATORS = {
    "valido": case_valido,
    "lat_fuera_de_rango": case_lat_fuera_de_rango,
    "lng_fuera_de_rango": case_lng_fuera_de_rango,
    "speed_negativo": case_speed_negativo,
    "campo_faltante": case_campo_faltante,
    "tipo_incorrecto": case_tipo_incorrecto,
    "vehicleId_formato_invalido": case_vehicleId_formato_invalido,
    "borde_valido": case_borde_valido,
}


def format_fields(event):
    """Alinea 'clave : valor' para que el JSON se lea como una tabla,
    no como un dict de una sola línea. Mismo estilo que consumer_demo.py,
    para que ambas terminales se vean consistentes."""
    if not event:
        return "      (mensaje vacío)"
    width = max(len(str(k)) for k in event.keys())
    return "\n".join(f"      {str(k).ljust(width)} : {v}" for k, v in event.items())


def send_one(producer, event, header=None):
    print(SEPARATOR)
    print(f"  {header}" if header else "  Enviando mensaje")
    print(SEPARATOR)
    print(format_fields(event))
    print()

    future = producer.send(TOPIC_NAME, value=event)
    producer.flush()
    record_metadata = future.get(timeout=10)
    print(f"      ✓ Confirmado por el broker: partición={record_metadata.partition} offset={record_metadata.offset}\n")


def main():
    parser = argparse.ArgumentParser()
    group = parser.add_mutually_exclusive_group()
    group.add_argument(
        "--json",
        dest="json_payload",
        default=None,
        help="JSON puntual a enviar. Si no se pasa nada (ni --count), usa el evento de ejemplo del reto.",
    )
    group.add_argument(
        "--count",
        dest="count",
        type=int,
        default=None,
        help="Cantidad de mensajes aleatorios a generar y enviar (mezcla de casos válidos/inválidos/borde).",
    )
    parser.add_argument(
        "--seed",
        dest="seed",
        type=int,
        default=None,
        help="Semilla para que la generación aleatoria sea reproducible (opcional, solo con --count).",
    )
    parser.add_argument(
        "--delay",
        dest="delay",
        type=float,
        default=0.3,
        help="Segundos de pausa entre mensajes cuando se usa --count (default 0.3s).",
    )
    args = parser.parse_args()

    if args.seed is not None:
        random.seed(args.seed)

    print(f"[PRODUCER-DEMO] Conectando a {KAFKA_BOOTSTRAP_SERVERS}...")
    producer = KafkaProducer(
        bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
        value_serializer=lambda v: json.dumps(v).encode("utf-8"),
    )

    if args.count:
        print(f"[PRODUCER-DEMO] Generando y enviando {args.count} mensajes aleatorios...\n")
        case_names = list(CASE_GENERATORS.keys())
        counts = {name: 0 for name in case_names}
        for i in range(1, args.count + 1):
            case_name = random.choice(case_names)
            counts[case_name] += 1
            event = CASE_GENERATORS[case_name]()
            send_one(producer, event, header=f"Mensaje {i}/{args.count} · caso: {case_name}")
            if i < args.count:
                time.sleep(args.delay)

        print(SEPARATOR)
        print("  Resumen de casos generados")
        print(SEPARATOR)
        name_width = max(len(n) for n in case_names)
        for name, n in counts.items():
            barra = "█" * n
            print(f"      {name.ljust(name_width)} : {str(n).rjust(2)}  {barra}")
        print(f"\n      Total enviados: {args.count}\n")
    else:
        event = json.loads(args.json_payload) if args.json_payload else DEFAULT_EVENT
        send_one(producer, event, header="Enviando mensaje")

    producer.close()


if __name__ == "__main__":
    main()
