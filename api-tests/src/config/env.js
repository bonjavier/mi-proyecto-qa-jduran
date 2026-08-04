// Configuración centralizada: nada de URLs/credenciales quemadas en los scripts.
// Todo es sobreescribible vía variables de entorno (k6 run -e VAR=valor ...),
// con defaults que apuntan a la API pública de DummyJSON para que la suite
// corra "out of the box" sin configuración adicional.

export const BASE_URL = __ENV.BASE_URL || 'https://dummyjson.com';

export const CREDENTIALS = {
  username: __ENV.API_USERNAME || 'emilys',
  password: __ENV.API_PASSWORD || 'emilyspass',
};

// SLA de rendimiento exigido por el reto: < 1.5s por petición.
export const SLA_MS = Number(__ENV.SLA_MS || 1500);
