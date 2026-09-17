package pedidos_service.dto;

import pedidos_service.entity.EstadoPedido;

import java.time.LocalDateTime;
import java.util.List;

public class PedidoDTO {

    private Long id;
    private LocalDateTime fecha;
    private EstadoPedido estado;
    private Long clienteId;
    private List<ItemPedidoDTO> items;

    public PedidoDTO() {
    }

    public PedidoDTO(Long id, LocalDateTime fecha, EstadoPedido estado, Long clienteId, List<ItemPedidoDTO> items) {
        this.id = id;
        this.fecha = fecha;
        this.estado = estado;
        this.clienteId = clienteId;
        this.items = items;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public void setEstado(EstadoPedido estado) {
        this.estado = estado;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public List<ItemPedidoDTO> getItems() {
        return items;
    }

    public void setItems(List<ItemPedidoDTO> items) {
        this.items = items;
    }
}