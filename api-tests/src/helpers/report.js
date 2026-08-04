import { htmlReport } from 'https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.2/index.js';

// Genera el reporte HTML/JSON con un nombre propio por script (functional-
// summary vs load-summary), para que correr uno no le pise el reporte al
// otro y el orquestador pueda mostrar ambos resultados por separado.
export function buildSummary(data, reportName) {
  return {
    [`reports/${reportName}.html`]: htmlReport(data),
    [`reports/${reportName}.json`]: JSON.stringify(data, null, 2),
    stdout: textSummary(data, { indent: ' ', enableColors: true }),
  };
}
