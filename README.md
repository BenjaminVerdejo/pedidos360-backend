# Pedidos360 - Backend

Backend de Pedidos360: microservicios Java 17 + Spring Boot 3, seguridad con JWT de Azure AD (IDaaS),
PostgreSQL en AWS RDS y mensajeria asincrona con RabbitMQ.

| Microservicio | Puerto | Descripcion |
|---|---|---|
| [`pedidos-service`](pedidos-service) | 8080 | CRUD de pedidos. Productor y consumidores RabbitMQ (notificaciones, tickets, DLQ). |
| [`bff-service`](bff-service) | 8081 | Backend for Frontend: valida el JWT y reenvia al pedidos-service. |
| [`rabbit-admin-service`](rabbit-admin-service) | 8082 | API REST para crear y eliminar colas, exchanges y bindings. |

```
Angular --JWT--> bff-service --JWT--> pedidos-service --eventos--> RabbitMQ --> consumidores (notificaciones / tickets)
                                                                      |                 \--fallo--> DLQ --> log
rabbit-admin-service --(crea / elimina colas, exchanges, bindings)----+
```

## RabbitMQ

- **Cluster de 2 nodos** (`rabbitmq1`, `rabbitmq2`) definido en `docker-compose.yml` + `rabbitmq/cluster.conf`.
  Los servicios se conectan con `spring.rabbitmq.addresses=localhost:5672,localhost:5673`.
- **3 colas con su DLQ**: `notificaciones`, `tickets`, `documentos`.
- **Exchanges**: `pedidos.exchange` (topic), `pedidos.direct` (direct), `pedidos.dlx` (direct, dead letter).

Comprobar el cluster: `docker exec rabbitmq1 rabbitmqctl cluster_status`

## Ejecutar en local

```bash
docker compose up -d                 # cluster RabbitMQ de 2 nodos (consolas :15672 y :15673, admin/admin123)

cd pedidos-service
export DB_PASSWORD=********          # PowerShell:  $env:DB_PASSWORD="********"
./mvnw spring-boot:run               # Windows: .\mvnw.cmd spring-boot:run

cd ../bff-service && ./mvnw spring-boot:run
cd ../rabbit-admin-service && ./mvnw spring-boot:run
```

Pruebas de cada microservicio: `./mvnw test` (no necesitan RabbitMQ ni RDS).

Detalle de la mensajeria en [pedidos-service/README.md](pedidos-service/README.md) y de la API
administradora en [rabbit-admin-service/README.md](rabbit-admin-service/README.md).
