#!/usr/bin/env bash
# Orquestador de la suite de API.
#
# Uso:
#   ./run.sh functional   -> solo consume/valida los endpoints críticos (main-flow.js)
#   ./run.sh load         -> solo la prueba de carga (load-test.js)
#   ./run.sh all          -> funcional primero; si pasa, lanza la carga.
#                            Si el funcional falla, NO se lanza la carga
#                            (no tiene sentido cargar endpoints que ya
#                            sabemos que están rotos) y el reporte funcional
#                            queda regenerado reflejando el error.
#   (sin argumentos)      -> equivale a "all"

set -uo pipefail
cd "$(dirname "$0")"

MODE="${1:-all}"
shift || true
EXTRA_ARGS=("$@") # se reenvían tal cual a k6 (ej. -e BASE_URL=..., -e API_PASSWORD=...)

run_functional() {
  echo "== [1/1] Validación funcional de endpoints críticos =="
  k6 run "${EXTRA_ARGS[@]}" src/main-flow.js
}

run_load() {
  echo "== Prueba de carga =="
  k6 run "${EXTRA_ARGS[@]}" src/load-test.js
}

case "$MODE" in
  functional)
    run_functional
    exit $?
    ;;
  load)
    run_load
    exit $?
    ;;
  all)
    run_functional
    FUNCTIONAL_EXIT=$?

    if [ "$FUNCTIONAL_EXIT" -eq 0 ]; then
      echo ""
      echo "Validación funcional OK. Lanzando prueba de carga..."
      run_load
      exit $?
    else
      echo ""
      echo "Validación funcional FALLÓ (exit code $FUNCTIONAL_EXIT)."
      echo "No se lanza la prueba de carga."
      echo "Ver reports/functional-summary.html para el detalle del error."
      exit "$FUNCTIONAL_EXIT"
    fi
    ;;
  *)
    echo "Uso: ./run.sh [functional|load|all]"
    exit 1
    ;;
esac
