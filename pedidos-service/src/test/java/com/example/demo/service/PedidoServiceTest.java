package com.example.demo.service;

import com.example.demo.dto.ClienteRequest;
import com.example.demo.dto.PedidoRequest;
import com.example.demo.exception.TransicionEstadoInvalidaException;
import com.example.demo.messaging.kafka.PedidoKafkaProducer;
import com.example.demo.messaging.producer.PedidoEventPublisher;
import com.example.demo.model.Cliente;
import com.example.demo.model.EstadoPedido;
import com.example.demo.model.Pedido;
import com.example.demo.repository.ClienteRepository;
import com.example.demo.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Pruebas unitarias de la logica de negocio de pedidos (sin BD ni RabbitMQ, con Mockito). */
class PedidoServiceTest {

    private PedidoRepository pedidoRepository;
    private ClienteRepository clienteRepository;
    private PedidoEventPublisher publisher;
    private PedidoKafkaProducer kafka;
    private PedidoService service;

    @BeforeEach
    void setUp() {
        pedidoRepository = mock(PedidoRepository.class);
        clienteRepository = mock(ClienteRepository.class);
        publisher = mock(PedidoEventPublisher.class);
        kafka = mock(PedidoKafkaProducer.class);
        service = new PedidoService(pedidoRepository, clienteRepository, publisher, kafka);
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> {
            Pedido p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(10L);
            }
            return p;
        });
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static PedidoRequest request(String email) {
        return new PedidoRequest(new ClienteRequest("Ana", email), "  Pedido de prueba ", new BigDecimal("1990.50"));
    }

    private static Pedido pedidoEnEstado(EstadoPedido estado) {
        Cliente c = new Cliente();
        c.setNombre("Ana");
        c.setEmail("ana@test.cl");
        Pedido p = new Pedido();
        p.setId(5L);
        p.setCliente(c);
        p.setDescripcion("x");
        p.setTotal(BigDecimal.TEN);
        p.setEstado(estado.name());
        return p;
    }

    @Test
    void crearAsignaEstadoPendienteNormalizaDatosYPublicaEvento() {
        when(clienteRepository.findByEmail("ana@test.cl")).thenReturn(Optional.empty());

        Pedido creado = service.crear(request("  ANA@Test.cl "));

        assertEquals(10L, creado.getId());
        assertEquals("PENDIENTE", creado.getEstado());
        assertEquals("ana@test.cl", creado.getCliente().getEmail());
        assertEquals("Pedido de prueba", creado.getDescripcion());
        assertNotNull(creado.getFecha());
        verify(publisher).publicarPedidoCreado(creado);
        verify(kafka).publicarPedidoCreado(creado);
    }

    @Test
    void crearReutilizaClienteExistente() {
        Cliente existente = new Cliente();
        existente.setId(3L);
        existente.setNombre("Ana");
        existente.setEmail("ana@test.cl");
        when(clienteRepository.findByEmail("ana@test.cl")).thenReturn(Optional.of(existente));

        Pedido creado = service.crear(request("ana@test.cl"));

        assertSame(existente, creado.getCliente());
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void entregarPublicaCambioDeEstadoYComprobante() {
        when(pedidoRepository.findById(5L)).thenReturn(Optional.of(pedidoEnEstado(EstadoPedido.ENVIADO)));

        Pedido p = service.cambiarEstado(5L, EstadoPedido.ENTREGADO).orElseThrow();

        assertEquals("ENTREGADO", p.getEstado());
        verify(publisher).publicarCambioEstado(p);
        verify(publisher).publicarPedidoEntregado(p);
        verify(kafka).publicarCambioEstado(p, "ENVIADO");
    }

    @Test
    void enviarNoPublicaComprobanteDeEntrega() {
        when(pedidoRepository.findById(5L)).thenReturn(Optional.of(pedidoEnEstado(EstadoPedido.PENDIENTE)));

        service.cambiarEstado(5L, EstadoPedido.ENVIADO);

        verify(publisher).publicarCambioEstado(any());
        verify(publisher, never()).publicarPedidoEntregado(any());
    }

    @Test
    void pedidoEnEstadoFinalNoPuedeCambiar() {
        when(pedidoRepository.findById(5L)).thenReturn(Optional.of(pedidoEnEstado(EstadoPedido.ENTREGADO)));

        assertThrows(TransicionEstadoInvalidaException.class,
                () -> service.cambiarEstado(5L, EstadoPedido.PENDIENTE));
        verifyNoInteractions(publisher, kafka);
    }

    @Test
    void cambiarEstadoDePedidoInexistenteDevuelveVacio() {
        when(pedidoRepository.findById(99L)).thenReturn(Optional.empty());
        assertTrue(service.cambiarEstado(99L, EstadoPedido.ENVIADO).isEmpty());
    }

    @Test
    void eliminarPedidoInexistenteDevuelveFalse() {
        when(pedidoRepository.existsById(99L)).thenReturn(false);
        assertFalse(service.eliminar(99L));
        verify(pedidoRepository, never()).deleteById(any());
    }
}
