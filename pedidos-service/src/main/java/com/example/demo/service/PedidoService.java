package com.example.demo.service;

import com.example.demo.messaging.producer.PedidoEventPublisher;
import com.example.demo.model.Cliente;
import com.example.demo.model.Pedido;
import com.example.demo.repository.ClienteRepository;
import com.example.demo.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    public PedidoService(PedidoRepository pedidoRepository,
                         ClienteRepository clienteRepository,
                         PedidoEventPublisher eventPublisher) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public List<Pedido> listar() {
        return pedidoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Pedido> obtener(Long id) {
        return pedidoRepository.findById(id);
    }

    @Transactional
    public Pedido crear(Pedido pedido) {
        pedido.setId(null);
        Cliente datos = pedido.getCliente();
        Cliente cliente = clienteRepository.findByEmail(datos.getEmail()).orElseGet(() -> {
            datos.setId(null);
            return clienteRepository.save(datos);
        });
        pedido.setCliente(cliente);
        Pedido guardado = pedidoRepository.save(pedido);
        eventPublisher.publicarPedidoCreado(guardado);
        return guardado;
    }

    @Transactional
    public Optional<Pedido> cambiarEstado(Long id, String estado) {
        return pedidoRepository.findById(id).map(p -> {
            p.setEstado(estado);
            Pedido actualizado = pedidoRepository.save(p);
            eventPublisher.publicarCambioEstado(actualizado);
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