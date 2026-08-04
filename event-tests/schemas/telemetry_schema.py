# "Contrato de datos": una descripción formal de la FORMA que debe tener
# un mensaje para considerarse válido — qué campos son obligatorios, de
# qué tipo debe ser cada uno, y (aquí además) qué rangos son físicamente
# posibles. Es exactamente lo mismo que hacíamos con los schemas de la
# capa de API (ver api-tests/src/schemas/), pero aquí sí podemos usar la
# librería estándar `jsonschema` porque este test corre en Python normal
# (pytest), no en el runtime restringido de k6.
#
# jsonschema.validate() recorre el diccionario recibido y, por cada regla
# de aquí abajo, revisa si se cumple. Si una sola regla falla, lanza
# ValidationError con un mensaje describiendo exactamente qué campo y
# por qué no pasó — no hay que escribir esa lógica de comparación a mano.
TELEMETRY_SCHEMA = {
    "type": "object",
    "required": ["vehicleId", "lat", "lng", "speed"],
    "properties": {
        "vehicleId": {
            "type": "string",
        },
        "lat": {
            # "number" acepta tanto enteros como decimales (float).
            "type": "number",
            # Rango físicamente válido de latitud terrestre.
            "minimum": -90,
            "maximum": 90,
        },
        "lng": {
            "type": "number",
            # Rango físicamente válido de longitud terrestre.
            "minimum": -180,
            "maximum": 180,
        },
        "speed": {
            "type": "number",
            # Una velocidad no puede ser negativa.
            "minimum": 0,
        },
    },
}
