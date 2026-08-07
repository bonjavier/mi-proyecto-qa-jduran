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
            # Formato estricto del identificador de vehículo: exactamente
            # 3 letras mayúsculas, un guion, y exactamente 3 dígitos.
            # Ej. válido: "VEH-099". Ej. inválido: "VEH-99" (solo 2
            # dígitos), "veh-099" (minúsculas), "VEHICULO-099" (más de
            # 3 letras). jsonschema usa regex estilo ECMA-262 en "pattern".
            "pattern": "^[A-Z]{3}-[0-9]{3}$",
        },
        "lat": {
            # "number" acepta tanto enteros como decimales (float).
            "type": "number",
            # Rango físicamente válido de latitud terrestre. El signo
            # importa pero AMBOS son legítimos (positivo = hemisferio
            # norte, negativo = hemisferio sur) — no se restringe el
            # signo, solo el rango.
            "minimum": -90,
            "maximum": 90,
        },
        "lng": {
            "type": "number",
            # Rango físicamente válido de longitud terrestre. Igual que
            # lat: ambos signos son válidos (positivo = este de
            # Greenwich, negativo = oeste); Colombia, por ejemplo, cae
            # siempre en longitud negativa.
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
