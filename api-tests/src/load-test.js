import http from 'k6/http';
import { group, sleep } from 'k6';
import { BASE_URL, CREDENTIALS } from './config/env.js';
import { checkStatus, checkSLA } from './helpers/checks.js';
import { buildSummary } from './helpers/report.js';

// Prueba de carga PEQUEÑA y deliberadamente conservadora. DummyJSON es una
// API pública compartida (sandbox de terceros), no infraestructura propia:
// no correspondería someterla a una carga real. El objetivo aquí es
// demostrar el patrón (ramp-up → sostenido → ramp-down, thresholds bajo
// carga) sobre los mismos endpoints críticos que ya se validaron
// funcionalmente en main-flow.js, no un benchmark real de capacidad.
export const options = {
  stages: [
    { duration: '10s', target: 5 },
    { duration: '20s', target: 10 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(95)<1500'],
    // Bajo carga se tolera algo más de variabilidad que en la validación
    // funcional (que exige rate>0.99): aquí basta con rate>0.95.
    checks: ['rate>0.95'],
  },
};

export default function () {
  group('LOAD - Login', function () {
    const res = http.post(
      `${BASE_URL}/user/login`,
      JSON.stringify({
        username: CREDENTIALS.username,
        password: CREDENTIALS.password,
        expiresInMins: 30,
      }),
      { headers: { 'Content-Type': 'application/json' } }
    );
    checkStatus(res, 200, 'LOAD - Login');
    checkSLA(res, 'LOAD - Login');
  });

  group('LOAD - Get user (read)', function () {
    const id = Math.floor(Math.random() * 100) + 1;
    const res = http.get(`${BASE_URL}/users/${id}`);
    checkStatus(res, 200, 'LOAD - Get user');
    checkSLA(res, 'LOAD - Get user');
  });

  group('LOAD - Add user (write)', function () {
    const suffix = `${__VU}-${__ITER}-${Date.now()}`;
    const res = http.post(
      `${BASE_URL}/users/add`,
      JSON.stringify({
        firstName: `Load-${suffix}`,
        lastName: `Test-${suffix}`,
        age: 25,
      }),
      { headers: { 'Content-Type': 'application/json' } }
    );
    checkStatus(res, 201, 'LOAD - Add user');
    checkSLA(res, 'LOAD - Add user');
  });

  sleep(1); // pacing entre iteraciones: evita ráfagas artificiales por VU
}

export function handleSummary(data) {
  return buildSummary(data, 'load-summary');
}
