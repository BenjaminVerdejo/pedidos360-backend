// Prueba de rendimiento (no funcional) con k6 - Pedidos360
// Ejecutar: k6 run -e BASE_URL=http://localhost:8081 pruebas/rendimiento/k6-humo.js
// Criterios de aceptacion (thresholds): p95 < 500 ms y menos de 1% de errores.
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
const TOKEN = __ENV.TOKEN || '';

// 401 es una respuesta esperada si no se entrega TOKEN (mide el costo de validar el JWT)
http.setResponseCallback(http.expectedStatuses(200, 401));

export const options = {
  scenarios: {
    carga: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 20 },
        { duration: '30s', target: 20 },
        { duration: '10s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<500'],
    http_req_failed: ['rate<0.01'],
    checks: ['rate>0.99'],
  },
};

export default function () {
  const salud = http.get(`${BASE_URL}/actuator/health`);
  check(salud, { 'health responde 200': (r) => r.status === 200 });

  const params = TOKEN ? { headers: { Authorization: `Bearer ${TOKEN}` } } : {};
  const pedidos = http.get(`${BASE_URL}/api/pedidos`, params);
  check(pedidos, {
    'API responde (200 con token / 401 sin token)': (r) => r.status === 200 || r.status === 401,
    'API responde en menos de 500 ms': (r) => r.timings.duration < 500,
  });
  sleep(1);
}
