# pedidos-service (Pedidos360)

Microservicio Spring Boot de pedidos: API REST protegida con JWT de Azure AD, PostgreSQL en AWS RDS
y mensajeria asincrona con RabbitMQ.

## Ejecutar

```bash
docker compose up -d   # (desde la raiz del repo) cluster RabbitMQ de 2 nodos (consolas :15672 y :15673, admin/admin123)
export DB_PASSWORD=********     # credenciales de RDS por variable de entorno (no van en el repo)
./mvnw spring-boot:run
./mvnw test                     # pruebas (H2 en memoria, no necesita RDS ni RabbitMQ)
```

## Mensajeria RabbitMQ

Todos los nombres estan en `application.properties` (prefijo `app.rabbit`) y se leen con
`MensajeriaProperties`. Ninguna clase de negocio escribe nombres de colas.

```
                         pedido.#        +----------------------+   nack   +-------------+     +-------------------+
PedidoService            +-------------->| notificaciones.queue |--------->| pedidos.dlx |---->| notificaciones.dlq|
   |                     |               +----------------------+          | (direct)    |     +-------------------+
   v                     |                                                 |             |
PedidoEventPublisher --> pedidos.exchange                                  |             |     +-------------------+
                     (topic)  pedido.creado  +--------------+     nack     |             |---->| tickets.dlq       |
                         +------------------>| tickets.queue|------------->|             |     +-------------------+
                                             +--------------+              |             |     +-------------------+
                     pedidos.direct (direct)  pedido.entregado +------------------+  |             |---->| documentos.dlq    |
                         +-------------------------------->| documentos.queue |->|             |     +-------------------+
                                                           +------------------+  +-------------+
```

Exchanges: `pedidos.exchange` (**topic**, enruta por patron), `pedidos.direct` (**direct**, clave exacta) y `pedidos.dlx` (**direct**, mensajes fallidos).

| Caso de uso | Routing key | Cola | Consumidor | Servicio de negocio |
|---|---|---|---|---|
| Notificar al cliente (pedido creado o cambio de estado) | `pedido.#` | `notificaciones.queue` | `NotificacionConsumer` | `NotificacionService` |
| Generar ticket del pedido | `pedido.creado` (topic) | `tickets.queue` | `TicketConsumer` | `TicketService` |
| Comprobante de entrega (pedido ENTREGADO) | `pedido.entregado` (direct `pedidos.direct`) | `documentos.queue` | `DocumentoConsumer` | `DocumentoService` |
| Registrar mensajes no entregados | (DLX `pedidos.dlx`) | `notificaciones.dlq`, `tickets.dlq`, `documentos.dlq` | `DeadLetterConsumer` | log |

### Estructura

```
messaging/
  config/     RabbitConfig (beans Queue/Exchange/Binding + fabrica de listeners), MensajeriaProperties
  producer/   PedidoEventPublisher
  dto/        PedidoEvento (mensaje JSON)
  consumer/
    notificaciones/  NotificacionConsumer
    tickets/         TicketConsumer
    documentos/      DocumentoConsumer
    dlq/             DeadLetterConsumer
    support/         ConfirmadorMensajes (politica ACK/NACK)
exception/    ErrorRecuperableException, ErrorNoRecuperableException
service/      PedidoService, NotificacionService, TicketService, DocumentoService  (no conocen RabbitMQ)
```

### ACK y manejo de errores

ACK **manual** (`AcknowledgeMode.MANUAL`, `defaultRequeueRejected=false`). `ConfirmadorMensajes` decide:

| Resultado | Accion |
|---|---|
| Procesado OK | `basicAck` |
| `ErrorRecuperableException` (servicio externo caido) | reintento con espera (`max-reintentos`, `espera-ms`) |
| Reintentos agotados | `basicNack(requeue=false)` -> DLX -> DLQ |
| `ErrorNoRecuperableException` (datos invalidos) | `basicNack(requeue=false)` directo a la DLQ, sin reintentar |
| Excepcion inesperada | se trata como no recuperable -> DLQ |

Nunca se usa `requeue=true`, asi un mensaje defectuoso no queda en un bucle infinito.
`DeadLetterConsumer` registra en el log la cola de origen, el motivo (`x-death`) y el contenido.

### Probar los errores en la demo

- Crear un pedido con `#falla-correo` en la descripcion: la notificacion falla 4 veces (1 + 3 reintentos) y termina en `notificaciones.dlq`.
- Crear un pedido con `#falla-ticket` en la descripcion: igual con `tickets.dlq`.
- Crear un pedido con `#falla-documento` y marcarlo **Entregado**: el comprobante termina en `documentos.dlq`.
- El ticket si se genera en el primer caso y la notificacion en el segundo: cada cola es independiente.
- Si RabbitMQ esta caido, el pedido igual se guarda (el publicador registra el error en el log).
