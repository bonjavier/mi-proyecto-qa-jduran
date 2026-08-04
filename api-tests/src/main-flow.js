import http from 'k6/http';
import { group, check } from 'k6';
import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';
import { BASE_URL, CREDENTIALS, SLA_MS } from './config/env.js';
import { loginSchema } from './schemas/loginSchema.js';
import { userSchema } from './schemas/userSchema.js';
import { addUserSchema } from './schemas/addUserSchema.js';
import { checkStatus, checkSLA, checkSchema, checkHeader } from './helpers/checks.js';

export const options = {
  vus: 2,
  iterations: 2,
  thresholds: {
    // p(95) en vez de max: el SLA describe la experiencia de la gran
    // mayoría de las peticiones. Un único outlier (cold start, blip de red)
    // no debería tumbar la corrida completa; p(95) sí detecta degradación
    // real y sostenida del rendimiento.
    http_req_duration: ['p(95)<1500'],
    checks: ['rate>0.99'],
  },
};

export default function () {
  let accessToken;

  group('01 - Login', function () {
    const res = http.post(
      `${BASE_URL}/user/login`,
      JSON.stringify({
        username: CREDENTIALS.username,
        password: CREDENTIALS.password,
        expiresInMins: 30,
      }),
      { headers: { 'Content-Type': 'application/json' } }
    );

    checkStatus(res, 200, '01 - Login');
    checkHeader(res, 'Content-Type', 'application/json', '01 - Login');
    checkSchema(res, loginSchema, '01 - Login');
    checkSLA(res, '01 - Login');

    const body = res.json();
    accessToken = body.accessToken;
  });

  group('02 - Get auth user', function () {
    const res = http.get(`${BASE_URL}/user/me`, {
      headers: { Authorization: `Bearer ${accessToken}` },
    });

    checkStatus(res, 200, '02 - Get auth user');
    checkSchema(res, userSchema, '02 - Get auth user');
    checkSLA(res, '02 - Get auth user');

    const body = res.json();
    check(res, {
      '02 - Get auth user: id is 1': () => body.id === 1,
      '02 - Get auth user: username is emilys': () => body.username === 'emilys',
    });
  });

  group('03 - Get single user', function () {
    const res = http.get(`${BASE_URL}/users/1`);

    checkStatus(res, 200, '03 - Get single user');
    checkSchema(res, userSchema, '03 - Get single user');
    checkSLA(res, '03 - Get single user');
  });

  // Nota: DummyJSON es un mock — POST/PUT/DELETE simulan la operación y
  // devuelven un objeto coherente, pero NO persisten. Un GET posterior por
  // id no reflejará el cambio (el creado no existirá, el editado no tendrá
  // el nuevo valor). Por eso cada mutación se valida contra su propia
  // respuesta inmediata, nunca releyendo el recurso después.
  group('04 - Mutaciones (comportamiento MOCK)', function () {
    // Payload dinámico: varía por VU/iteración para no repetir el mismo
    // valor en corridas con más de 1 VU o iteración.
    const uniqueSuffix = `${__VU}-${__ITER}-${Date.now()}`;
    const newUser = {
      firstName: `Test-${uniqueSuffix}`,
      lastName: `User-${uniqueSuffix}`,
      age: 20 + (__ITER % 40),
    };

    const addRes = http.post(`${BASE_URL}/users/add`, JSON.stringify(newUser), {
      headers: { 'Content-Type': 'application/json' },
    });

    checkStatus(addRes, 201, '04 - POST users/add');
    checkSchema(addRes, addUserSchema, '04 - POST users/add');
    checkSLA(addRes, '04 - POST users/add');
    const addBody = addRes.json();
    check(addRes, {
      '04 - POST users/add: returns a new id': () => typeof addBody.id === 'number',
      '04 - POST users/add: echoes firstName sent': () => addBody.firstName === newUser.firstName,
      '04 - POST users/add: echoes lastName sent': () => addBody.lastName === newUser.lastName,
    });

    const updatedLastName = `Updated-${uniqueSuffix}`;
    const putRes = http.put(
      `${BASE_URL}/users/2`,
      JSON.stringify({ lastName: updatedLastName }),
      { headers: { 'Content-Type': 'application/json' } }
    );

    checkStatus(putRes, 200, '04 - PUT users/2');
    checkSchema(putRes, userSchema, '04 - PUT users/2');
    checkSLA(putRes, '04 - PUT users/2');
    const putBody = putRes.json();
    check(putRes, {
      '04 - PUT users/2: lastName reflects update': () => putBody.lastName === updatedLastName,
    });

    const deleteRes = http.del(`${BASE_URL}/users/1`);

    checkStatus(deleteRes, 200, '04 - DELETE users/1');
    checkSLA(deleteRes, '04 - DELETE users/1');
    const deleteBody = deleteRes.json();
    check(deleteRes, {
      '04 - DELETE users/1: isDeleted is true': () => deleteBody.isDeleted === true,
      '04 - DELETE users/1: deletedOn is present': () => !!deleteBody.deletedOn,
    });
  });

  // Escenario de demostración de SLA negativo: se suma al flujo normal solo
  // bajo demanda (k6 run -e RUN_DELAY_DEMO=true ...), para que el mismo
  // reporte muestre checks en verde junto a una falla real, en vez de
  // generar una corrida aparte que sobrescribe el reporte completo.
  if (__ENV.RUN_DELAY_DEMO === 'true') {
    group('05 - DEMO - SLA negativo (?delay=2000)', function () {
      const res = http.get(`${BASE_URL}/users/1?delay=2000`);
      checkStatus(res, 200, '05 - DEMO - delay');
      // Este check DEBE fallar: demuestra que la aserción de SLA detecta
      // una respuesta lenta real, no solo que "compila".
      checkSLA(res, '05 - DEMO - delay');
    });
  }
}

export function handleSummary(data) {
  return {
    'reports/summary.html': htmlReport(data),
    'reports/summary.json': JSON.stringify(data, null, 2),
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}
