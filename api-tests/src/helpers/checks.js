import { check } from 'k6';
import { SLA_MS } from '../config/env.js';

// Validador liviano tipo JSON Schema (campos requeridos + tipo por typeof).
// Ver README.md ("Decisiones de diseño") para el porqué de este enfoque
// en vez de bundlear ajv: ajv compila validadores generando código en
// runtime (`new Function`), un patrón con problemas de compatibilidad
// conocidos sobre el motor JS de k6 (goja, no V8).
export function matchesSchema(body, schema) {
  if (!body || typeof body !== 'object') return false;

  for (const field of schema.required) {
    if (!(field in body)) return false;
  }

  for (const [field, type] of Object.entries(schema.properties || {})) {
    if (body[field] === undefined) continue; // opcional: si no vino, no se valida el tipo
    if (type === 'array') {
      if (!Array.isArray(body[field])) return false;
    } else if (typeof body[field] !== type) {
      return false;
    }
  }

  return true;
}

export function checkStatus(res, expectedStatus, label) {
  return check(res, {
    [`${label}: status is ${expectedStatus}`]: (r) => r.status === expectedStatus,
  });
}

export function checkSLA(res, label, thresholdMs = SLA_MS) {
  return check(res, {
    [`${label}: response time < ${thresholdMs}ms`]: (r) => r.timings.duration < thresholdMs,
  });
}

export function checkSchema(res, schema, label) {
  return check(res, {
    [`${label}: body matches schema`]: (r) => {
      try {
        return matchesSchema(r.json(), schema);
      } catch (e) {
        return false;
      }
    },
  });
}

// Búsqueda case-insensitive: k6 conserva el casing original de las
// cabeceras del servidor, que no siempre coincide con el nombre canónico.
export function checkHeader(res, headerName, expectedSubstring, label) {
  return check(res, {
    [`${label}: header ${headerName} includes "${expectedSubstring}"`]: (r) => {
      const foundKey = Object.keys(r.headers).find(
        (k) => k.toLowerCase() === headerName.toLowerCase()
      );
      const value = foundKey ? r.headers[foundKey] : '';
      return value.toLowerCase().includes(expectedSubstring.toLowerCase());
    },
  });
}
