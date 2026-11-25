#!/usr/bin/env bash
# Pruebas de seguridad DINAMICAS (DAST scripted) contra los servicios en ejecucion.
# Uso: bash scripts/pruebas-seguridad-dinamicas.sh   (requiere el stack levantado)
#   PEDIDOS_URL (por defecto http://localhost:8080)  BFF_URL (por defecto http://localhost:8081)
# Imprime una tabla Markdown y termina con codigo 1 si alguna prueba falla.
set -u
PEDIDOS_URL="${PEDIDOS_URL:-http://localhost:8080}"
BFF_URL="${BFF_URL:-http://localhost:8081}"
FALLAS=0
TOTAL=0

registrar() { # id, descripcion, esperado, obtenido, ok(0/1)
  TOTAL=$((TOTAL + 1))
  if [ "$5" = "0" ]; then estado="APROBADA"; else estado="FALLIDA"; FALLAS=$((FALLAS + 1)); fi
  echo "| $1 | $2 | $3 | $4 | $estado |"
}

codigo() { curl -s -o /dev/null -w "%{http_code}" "$@"; }

b64url() { printf '%s' "$1" | base64 | tr -d '=' | tr '/+' '_-' | tr -d '\n'; }

echo "## Pruebas de seguridad dinamicas - $(date -u +%Y-%m-%dT%H:%M:%SZ)"
echo
echo "| ID | Prueba | Esperado | Obtenido | Resultado |"
echo "|---|---|---|---|---|"

for SVC in "pedidos-service|$PEDIDOS_URL" "bff-service|$BFF_URL"; do
  NOMBRE="${SVC%%|*}"; URL="${SVC##*|}"

  c=$(codigo "$URL/actuator/health")
  registrar "DAST-01" "$NOMBRE: health publico disponible" "200" "$c" "$([ "$c" = 200 ] && echo 0 || echo 1)"

  cuerpo=$(curl -s "$URL/actuator/health")
  registrar "DAST-02" "$NOMBRE: health no expone detalles internos" "sin 'components'" \
    "$(echo "$cuerpo" | grep -q components && echo 'expone' || echo 'no expone')" \
    "$(echo "$cuerpo" | grep -q components && echo 1 || echo 0)"

  c=$(codigo "$URL/api/pedidos")
  registrar "DAST-03" "$NOMBRE: API sin token" "401" "$c" "$([ "$c" = 401 ] && echo 0 || echo 1)"

  c=$(codigo -H "Authorization: Bearer token-inventado-123" "$URL/api/pedidos")
  registrar "DAST-04" "$NOMBRE: token falso" "401" "$c" "$([ "$c" = 401 ] && echo 0 || echo 1)"

  # JWT sin firma (alg=none) con scope y rol Admin falsificados
  H=$(b64url '{"alg":"none","typ":"JWT"}')
  P=$(b64url '{"sub":"atacante","scp":"Pedidos.Access","roles":["Admin"],"exp":4102444800}')
  c=$(codigo -X DELETE -H "Authorization: Bearer $H.$P." "$URL/api/pedidos/1")
  registrar "DAST-05" "$NOMBRE: JWT alg=none falsificado con rol Admin" "401" "$c" "$([ "$c" = 401 ] && echo 0 || echo 1)"

  c=$(codigo "$URL/actuator/env")
  registrar "DAST-06" "$NOMBRE: /actuator/env no accesible" "401/404" "$c" "$([ "$c" = 401 ] || [ "$c" = 404 ] && echo 0 || echo 1)"

  cab=$(curl -s -D - -o /dev/null "$URL/api/pedidos")
  for h in "X-Content-Type-Options: nosniff" "X-Frame-Options: DENY" "Content-Security-Policy" "Referrer-Policy"; do
    echo "$cab" | grep -qi "^$h" && ok=0 || ok=1
    registrar "DAST-07" "$NOMBRE: cabecera $h" "presente" "$([ $ok = 0 ] && echo presente || echo ausente)" "$ok"
  done

  c=$(codigo -X OPTIONS -H "Origin: https://sitio-atacante.com" -H "Access-Control-Request-Method: GET" "$URL/api/pedidos")
  registrar "DAST-08" "$NOMBRE: CORS desde origen no autorizado" "403" "$c" "$([ "$c" = 403 ] && echo 0 || echo 1)"

  c=$(codigo -X TRACE "$URL/api/pedidos")
  registrar "DAST-09" "$NOMBRE: metodo TRACE deshabilitado" "!=200" "$c" "$([ "$c" != 200 ] && echo 0 || echo 1)"

  cuerpo=$(curl -s -H "Authorization: Bearer" "$URL/api/pedidos")
  echo "$cuerpo" | grep -Eqi "exception|stacktrace|at com\.|at org\." && ok=1 || ok=0
  registrar "DAST-10" "$NOMBRE: errores sin trazas internas" "sin stacktrace" "$([ $ok = 0 ] && echo 'sin trazas' || echo 'con trazas')" "$ok"
done

echo
echo "**Total: $TOTAL pruebas | Aprobadas: $((TOTAL - FALLAS)) | Fallidas: $FALLAS**"
[ -n "${GITHUB_STEP_SUMMARY:-}" ] && echo "DAST scripted: $((TOTAL - FALLAS))/$TOTAL aprobadas" >> "$GITHUB_STEP_SUMMARY"
[ "$FALLAS" -eq 0 ]
