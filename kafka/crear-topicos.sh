#!/usr/bin/env bash
# Crea los topicos de Pedidos360 con sus parametros de administracion (idempotente: --if-not-exists).
# Lo ejecuta el contenedor kafka-init de docker-compose cuando los 3 brokers estan sanos.
# Tambien se puede correr a mano:  docker compose --profile kafka run --rm kafka-init
set -euo pipefail

BOOTSTRAP="${BOOTSTRAP:-kafka1:9092,kafka2:9092,kafka3:9092}"
PARTICIONES=3
REPLICAS=3

echo "Esperando a que el cluster responda en ${BOOTSTRAP}..."
until kafka-topics --bootstrap-server "$BOOTSTRAP" --list > /dev/null 2>&1; do
  sleep 2
done

crear() {
  local topico="$1"; shift
  kafka-topics --bootstrap-server "$BOOTSTRAP" --create --if-not-exists \
    --topic "$topico" --partitions "$PARTICIONES" --replication-factor "$REPLICAS" "$@"
  echo "OK  $topico"
}

# Historial de pedidos creados: borrado por tiempo (7 dias), 2 replicas deben confirmar cada escritura
crear pedidos-creados \
  --config cleanup.policy=delete \
  --config retention.ms=604800000 \
  --config min.insync.replicas=2

# Estado de pedidos: compactado (Kafka conserva el ultimo estado de cada pedido = clave)
crear pedidos-estados \
  --config cleanup.policy=compact \
  --config min.compaction.lag.ms=60000 \
  --config segment.ms=3600000 \
  --config min.insync.replicas=2

# Topicos de mensajes fallidos (Dead Letter Topics) de analitica-service: 14 dias para investigarlos
crear pedidos-creados.DLT --config cleanup.policy=delete --config retention.ms=1209600000
crear pedidos-estados.DLT --config cleanup.policy=delete --config retention.ms=1209600000

echo
echo "Topicos creados:"
kafka-topics --bootstrap-server "$BOOTSTRAP" --describe --topic pedidos-creados
kafka-topics --bootstrap-server "$BOOTSTRAP" --describe --topic pedidos-estados
