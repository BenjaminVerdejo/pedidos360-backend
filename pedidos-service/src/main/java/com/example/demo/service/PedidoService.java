package com.example.demo.service;

import com.example.demo.dto.PedidoRequest;
import com.example.demo.exception.TransicionEstadoInvalidaException;
import com.example.demo.messaging.kafka.PedidoKafkaProducer;
import com.example.demo.messaging.producer.PedidoEventPublisher;
import com.example.demo.model.Cliente;
import com.example.demo.model.EstadoPedido;
import com.example.demo.model.Pedido;
import com.example.demo.repository.ClienteRepository;
import com.example.demo.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Logica de negocio de pedidos. El controller solo delega aqui.
 * La mensajeria se usa a traves del publicador; este servicio no conoce colas ni exchanges.
 */
@Service
public class PedidoService {

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final PedidoEventPublisher eventPublisher;
    private final PedidoKafkaProducer kafkaProducer;

    public PedidoService(PedidoRepository pedidoRepository,
                         ClienteRepository clienteRepository,
                         PedidoEventPublisher eventPublisher,
                         PedidoKafkaProducer kafkaProducer) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.eventPublisher = eventPublisher;
        this.kafkaProducer = kafkaProducer;
    }

    @Transactional(readOnly = true)
    public List<Pedido> listar() {
        return pedidoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Pedido> obtener(Long id) {
        return pedidoRepository.findById(id);
    }

    /**
     * Crea un pedido a partir del DTO. id, estado y fecha los define SIEMPRE el backend
     * (el cliente no puede crear un pedido ya "ENTREGADO" ni con otra fecha).
     */
    @Transactional
    public Pedido crear(PedidoRequest request) {
        String email = request.cliente().email().trim().toLowerCase(Locale.ROOT);
        Cliente cliente = clienteRepository.findByEmail(email).orElseGet(() -> {
            Cliente nuevo = new Cliente();
            nuevo.setNombre(request.cliente().nombre().trim());
            nuevo.setEmail(email);
            return clienteRepository.save(nuevo);
        });

        Pedido pedido = new Pedido();
        pedido.setCliente(cliente);
        pedido.setDescripcion(request.descripcion().trim());
        pedido.setTotal(request.total());
        pedido.setEstado(EstadoPedido.PENDIENTE.name());
        pedido.setFecha(LocalDateTime.now());

        Pedido guardado = pedidoRepository.save(pedido);
        eventPublisher.publicarPedidoCreado(guardado);   // RabbitMQ: notificacion + ticket
        kafkaProducer.publicarPedidoCreado(guardado);    // Kafka: streaming para analitica/auditoria
        return guardado;
    }

    /**
     * Cambia el estado validando la transicion: ENTREGADO y CANCELADO son finales.
     * @throws TransicionEstadoInvalidaException si el pedido ya esta en un estado final
     */
    @Transactional
    public Optional<Pedido> cambiarEstado(Long id, EstadoPedido nuevoEstado) {
        return pedidoRepository.findById(id).map(p -> {
            EstadoPedido actual = EstadoPedido.desde(p.getEstado());
            if (actual.esFinal()) {
                throw new TransicionEstadoInvalidaException(
                        "El pedido #" + id + " esta " + actual + " y ya no puede cambiar de estado");
            }
            p.setEstado(nuevoEstado.name());
            Pedido actualizado = pedidoRepository.save(p);
            eventPublisher.publicarCambioEstado(actualizado);
            kafkaProducer.publicarCambioEstado(actualizado, actual.name());
            if (nuevoEstado == EstadoPedido.ENTREGADO) {
                eventPublisher.publicarPedidoEntregado(actualizado);
            }
            return actualizado;
        });
    }

    @Transactional
    public boolean eliminar(Long id) {
        if (!pedidoRepository.existsById(id)) {
            return false;
        }
        pedidoRepository.deleteById(id);
        return true;
    }
}
