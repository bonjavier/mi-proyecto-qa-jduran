"""
Test integrado: publica un evento de telemetría GPS y lo consume de vuelta
en el mismo flujo, validando integridad + contrato + rangos + timeout.

Requiere Kafka corriendo localmente (ver docker-compose.yml / README.md).
"""

import json
import time
import uuid

import pytest
from jsonschema import validate, ValidationError
from kafka import KafkaProducer, KafkaConsumer, KafkaAdminClient
from kafka.admin import NewTopic
from kafka.errors import TopicAlreadyExistsError

from schemas.telemetry_schema import TELEMETRY_SCHEMA

KAFKA_BOOTSTRAP_SERVERS = "localhost:9092"
TOPIC_NAME = "gps-raw-events"
CONSUME_TIMEOUT_SECONDS = 10

# El evento de ejemplo que pide el enunciado del reto, tal cual.
SAMPLE_EVENT = {
    "vehicleId": "VEH-99",
    "lat": 4.60,
    "lng": -74.08,
    "speed": 65,
}


@pytest.fixture(scope="module")
def ensure_topic_exists():
    """
    Kafka puede crear tópicos automáticamente al primer uso, pero eso es
    poco predecible en un test (puede crearlo "a medias" justo cuando el
    consumer intenta suscribirse). Lo creamos explícitamente antes de
    correr nada, y si ya existe de una corrida anterior, lo ignoramos:
    un tópico es barato de mantener y no necesitamos borrarlo entre corridas.
    """
    admin = KafkaAdminClient(bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS)
    try:
        admin.create_topics(
            [NewTopic(name=TOPIC_NAME, num_partitions=1, replication_factor=1)]
        )
    except TopicAlreadyExistsError:
        pass
    finally:
        admin.close()
    yield


def test_gps_event_is_produced_and_consumed_correctly(ensure_topic_exists):
    # ────────────────────────────────────────────────────────────────
    # PRODUCER: publica el evento
    # ────────────────────────────────────────────────────────────────
    # Kafka transporta y guarda mensajes como BYTES puros — no sabe nada
    # de JSON, Python ni de tu estructura de datos. `value_serializer` es
    # la función que se aplica automáticamente a cada mensaje antes de
    # enviarlo: convierte el dict de Python a texto JSON (json.dumps) y
    # ese texto a bytes (.encode). El consumer, del otro lado, tendrá que
    # hacer el camino inverso (bytes -> texto -> dict) para poder leerlo.
    producer = KafkaProducer(
        bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
        value_serializer=lambda v: json.dumps(v).encode("utf-8"),
    )

    producer.send(TOPIC_NAME, value=SAMPLE_EVENT)
    # send() es asíncrono (encola el mensaje y sigue); flush() bloquea
    # hasta que el broker confirmó que lo recibió. Sin flush(), el test
    # podría intentar consumir antes de que el mensaje siquiera saliera.
    producer.flush()
    producer.close()

    # ────────────────────────────────────────────────────────────────
    # CONSUMER: lee el evento de vuelta
    # ────────────────────────────────────────────────────────────────
    # OFFSET: cada mensaje en un tópico tiene una posición numérica
    # (offset) dentro de su partición, como el número de página de un
    # libro. Kafka recuerda, POR GRUPO DE CONSUMIDORES (group_id), hasta
    # qué offset ya leyó ese grupo. Si reutilizas el mismo group_id entre
    # corridas, la segunda vez Kafka piensa "este grupo ya leyó hasta la
    # página X" y solo te entrega mensajes NUEVOS después de esa página
    # — el mensaje que acabas de publicar en ESTA corrida, si coincide
    # con lo que ya se marcó como leído antes, puede quedar "saltado".
    #
    # Por eso generamos un group_id ÚNICO (uuid) en cada ejecución del
    # test: para ese grupo, Kafka no tiene ningún offset registrado
    # todavía, así que combinado con auto_offset_reset="earliest" el
    # consumer arranca desde el principio del tópico y SÍ ve el mensaje
    # que acabamos de producir, sin importar qué corrió antes.
    #
    # (El otro valor posible de auto_offset_reset es "latest": solo
    # mensajes producidos DESPUÉS de que el consumer se conectó — con eso
    # habría una carrera real entre producer y consumer y el test sería
    # flaky. "earliest" + group nuevo es la combinación que lo hace
    # determinista.)
    unique_group_id = f"test-consumer-{uuid.uuid4()}"

    # NOTA (problema real encontrado y corregido): en Windows, el selector
    # de sockets interno de kafka-python-ng a veces lanza
    # "ValueError: Invalid file descriptor: -1" durante el primer intento
    # de conexión del consumer al coordinador del grupo — una condición de
    # carrera de bajo nivel entre el cierre y el registro del socket, NO
    # un problema de nuestro tópico/offset (se confirmó corriendo el test
    # 8 veces seguidas: ~25% de las corridas fallaban con exactamente este
    # error, siempre en la misma línea de la librería). Un reintento simple
    # de la conexión lo resuelve, porque el segundo intento abre un socket
    # nuevo sin el estado corrupto del anterior.
    received_event = None
    last_error = None
    max_attempts = 5
    for attempt in range(1, max_attempts + 1):
        if attempt > 1:
            # Pequeña pausa antes de reintentar: le da tiempo al socket
            # roto del intento anterior a liberarse del todo a nivel de
            # sistema operativo antes de que la librería abra uno nuevo.
            time.sleep(1)
        try:
            consumer = KafkaConsumer(
                TOPIC_NAME,
                bootstrap_servers=KAFKA_BOOTSTRAP_SERVERS,
                auto_offset_reset="earliest",
                group_id=unique_group_id,
                value_deserializer=lambda v: json.loads(v.decode("utf-8")),
                # Si no llega ningún mensaje en este tiempo, dejar de esperar
                # (en vez de bloquear el test para siempre).
                consumer_timeout_ms=CONSUME_TIMEOUT_SECONDS * 1000,
            )
            for message in consumer:
                received_event = message.value
                break  # con el primero que llegue alcanza para este test
            consumer.close()
            break  # conexión y consumo OK, no hace falta reintentar
        except ValueError as e:
            last_error = e
            print(f"Intento {attempt}/{max_attempts} falló por un error de "
                  f"socket transitorio ({e}); reintentando...")
    else:
        pytest.fail(
            f"El consumer falló {max_attempts} veces seguidas por un error "
            f"de socket transitorio: {last_error}"
        )

    # ────────────────────────────────────────────────────────────────
    # ASERCIONES
    # ────────────────────────────────────────────────────────────────
    if received_event is None:
        pytest.fail(
            f"No se recibió ningún mensaje del tópico '{TOPIC_NAME}' en "
            f"{CONSUME_TIMEOUT_SECONDS}s. Verifica que Kafka esté corriendo "
            f"(docker compose up -d en event-tests/) y que el producer haya "
            f"publicado sin errores."
        )

    # 1) Integridad: lo que llegó debe ser EXACTAMENTE lo que se envió.
    assert received_event == SAMPLE_EVENT, (
        f"El evento recibido no coincide con el enviado.\n"
        f"Enviado:  {SAMPLE_EVENT}\n"
        f"Recibido: {received_event}"
    )

    # 2) Contrato: la estructura y los tipos deben cumplir el schema.
    try:
        validate(instance=received_event, schema=TELEMETRY_SCHEMA)
    except ValidationError as e:
        pytest.fail(f"El evento no cumple el schema de telemetría: {e.message}")

    # 3) Rangos válidos (redundante con el schema, pero explícito y
    #    fácil de leer/mostrar en el video sin abrir el archivo de schema).
    assert -90 <= received_event["lat"] <= 90, "Latitud fuera de rango válido"
    assert -180 <= received_event["lng"] <= 180, "Longitud fuera de rango válido"
    assert received_event["speed"] >= 0, "Velocidad no puede ser negativa"
