# rabbit-admin-service (Pedidos360)

Microservicio administrador de RabbitMQ: crea y elimina **colas, exchanges y bindings** por API REST.
Protegido con JWT de Azure AD: requiere el scope `Pedidos.Access` **y** el rol `Admin`.

## Ejecutar

```bash
# RabbitMQ: docker compose up -d desde la raiz del repo
./mvnw spring-boot:run      # puerto 8082
./mvnw test
```

## Diseno

- `controller/`: `ColaController`, `ExchangeController`, `BindingController`. Solo validan y delegan.
- `service/RabbitAdminService`: **toda** la logica de RabbitMQ (unica clase que usa `RabbitAdmin`).
- `dto/`: requests con Bean Validation (`@NotBlank`, `@Pattern`, `@Positive`, `@Max`, `@AssertTrue`).
- `exception/GlobalExceptionHandler`: errores JSON claros y uniformes.
- `application.yml`: conexion y lista de colas/exchanges **protegidos** (la topologia de pedidos-service no se puede borrar).

## Endpoints

| Metodo | Ruta | Descripcion | Respuestas |
|---|---|---|---|
| POST | `/api/admin/queues` | Crear cola | 201, 400, 404 (DLX no existe), 409 (ya existe) |
| GET | `/api/admin/queues/{nombre}` | Estado (mensajes y consumidores) | 200, 400, 404 |
| DELETE | `/api/admin/queues/{nombre}?soloSiVacia=true` | Eliminar cola | 204, 400, 404, 409 (protegida o con mensajes) |
| POST | `/api/admin/exchanges` | Crear exchange | 201, 400, 409 |
| DELETE | `/api/admin/exchanges/{nombre}` | Eliminar exchange | 204, 400, 404, 409 (protegido) |
| POST | `/api/admin/bindings` | Crear binding exchange -> cola | 201, 400, 404 |
| DELETE | `/api/admin/bindings?exchange=&cola=&routingKey=` | Eliminar binding | 204, 400, 404 |

Sin token: 401. Sin rol Admin: 403. RabbitMQ caido: 503.

### Validaciones

- Nombres obligatorios, maximo 255 caracteres, solo `A-Z a-z 0-9 . - _` (sin espacios).
- No se permite el prefijo `amq.` (reservado por RabbitMQ).
- `ttlMs` entre 1 y 604800000 (7 dias); `maxLength` entre 1 y 1.000.000.
- `deadLetterRoutingKey` requiere `deadLetterExchange`, y ese exchange debe existir.
- `tipo` de exchange: `DIRECT`, `TOPIC`, `FANOUT` o `HEADERS`.
- `routingKey` admite `*` y `#` (topic) y puede ser `""` (fanout).

### Ejemplos

```bash
TOKEN=...   # access token de Azure AD con rol Admin

curl -X POST localhost:8082/api/admin/exchanges -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"nombre":"facturas.exchange","tipo":"TOPIC"}'

curl -X POST localhost:8082/api/admin/queues -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"nombre":"facturas.queue","ttlMs":60000,"deadLetterExchange":"pedidos.dlx","deadLetterRoutingKey":"facturas.dlq"}'

curl -X POST localhost:8082/api/admin/bindings -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"exchange":"facturas.exchange","cola":"facturas.queue","routingKey":"factura.#"}'

curl -X DELETE "localhost:8082/api/admin/bindings?exchange=facturas.exchange&cola=facturas.queue&routingKey=factura.%23" \
  -H "Authorization: Bearer $TOKEN"
curl -X DELETE localhost:8082/api/admin/queues/facturas.queue -H "Authorization: Bearer $TOKEN"
curl -X DELETE localhost:8082/api/admin/exchanges/facturas.exchange -H "Authorization: Bearer $TOKEN"

# Error de validacion (400):
curl -X POST localhost:8082/api/admin/queues -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" -d '{"nombre":""}'
# {"status":400,"error":"validacion","mensaje":"La solicitud tiene datos invalidos",
#  "campos":{"nombre":"el nombre de la cola es obligatorio"}, ...}
```
