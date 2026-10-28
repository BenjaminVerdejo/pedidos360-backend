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

## Ejecutar en local

```bash
docker compose up -d                 # RabbitMQ  (consola: http://localhost:15672  admin/admin123)

cd pedidos-service
export DB_PASSWORD=********          # PowerShell:  $env:DB_PASSWORD="********"
./mvnw spring-boot:run               # Windows: .\mvnw.cmd spring-boot:run

cd ../bff-service && ./mvnw spring-boot:run
cd ../rabbit-admin-service && ./mvnw spring-boot:run
```

Pruebas de cada microservicio: `./mvnw test` (no necesitan RabbitMQ ni RDS).

Detalle de la mensajeria en [pedidos-service/README.md](pedidos-service/README.md) y de la API
administradora en [rabbit-admin-service/README.md](rabbit-admin-service/README.md).
