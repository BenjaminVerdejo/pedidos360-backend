# Pedidos360 - Backend

[![CI Seguridad y Calidad](https://github.com/BenjaminVerdejo/pedidos360-backend/actions/workflows/ci-seguridad.yml/badge.svg)](https://github.com/BenjaminVerdejo/pedidos360-backend/actions/workflows/ci-seguridad.yml)
[![CodeQL](https://github.com/BenjaminVerdejo/pedidos360-backend/actions/workflows/codeql.yml/badge.svg)](https://github.com/BenjaminVerdejo/pedidos360-backend/actions/workflows/codeql.yml)

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
cp .env.example .env                 # completar RABBIT_PASSWORD, RABBIT_COOKIE y DB_PASSWORD (sin valores por defecto)
docker compose up -d                 # cluster RabbitMQ de 2 nodos (consolas :15672 y :15673)
docker compose --profile app up -d --build   # (opcional) todo el stack en Docker: RabbitMQ + PostgreSQL + 3 microservicios

cd pedidos-service
export DB_HOST=... DB_PASSWORD=... RABBIT_PASSWORD=...   # PowerShell:  $env:DB_PASSWORD="..."
./mvnw spring-boot:run               # Windows: .\mvnw.cmd spring-boot:run

cd ../bff-service && ./mvnw spring-boot:run
cd ../rabbit-admin-service && ./mvnw spring-boot:run
```

Pruebas de cada microservicio: `./mvnw verify` (no necesitan RabbitMQ ni RDS). Reporte de cobertura en
`target/site/jacoco/index.html`.

## Seguridad y calidad (EP5)

Cada *push* / *pull request* ejecuta el pipeline DevSecOps [`ci-seguridad.yml`](.github/workflows/ci-seguridad.yml):

| Etapa | Herramientas |
|---|---|
| Pruebas automatizadas + cobertura | JUnit 5, MockMvc, Mockito, Spring Security Test, JaCoCo |
| SAST | Semgrep (reglas propias en [`.semgrep/`](.semgrep) + OWASP Top 10) y CodeQL |
| Secretos | Gitleaks (todo el historial) |
| SCA | Trivy + Dependabot |
| Contenedores | Hadolint + Trivy imagen |
| DAST + rendimiento | [`scripts/pruebas-seguridad-dinamicas.sh`](scripts/pruebas-seguridad-dinamicas.sh), OWASP ZAP, k6 |

Controles en el codigo: DTO con Bean Validation (sin asignacion masiva), estados de pedido como enum con
transiciones validas, manejo global de errores sin trazas, cabeceras de seguridad HTTP, CORS restringido,
lista blanca de rutas y *timeouts* en el BFF, emails enmascarados en logs y log de auditoria.

- [docs/seguridad-y-calidad.md](docs/seguridad-y-calidad.md): diagramas del pipeline y matriz normativa (OWASP, ISO 27001, Ley 21.719 / 21.663)
- [docs/plan-mantenimiento.md](docs/plan-mantenimiento.md): plan de mantenimiento y KPI
- [SECURITY.md](SECURITY.md): politica de reporte de vulnerabilidades

Detalle de la mensajeria en [pedidos-service/README.md](pedidos-service/README.md) y de la API
administradora en [rabbit-admin-service/README.md](rabbit-admin-service/README.md).
