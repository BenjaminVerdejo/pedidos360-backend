package pedidos_service.dto;

import java.math.BigDecimal;

public class ItemPedidoDTO {

    private Long id;
    private String nombreProducto;
    private Integer cantidad;
    private BigDecimal precioUnitario;

    public ItemPedidoDTO() {
    }

    public ItemPedidoDTO(Long id, String nombreProducto, Integer cantidad, BigDecimal precioUnitario) {
        this.id = id;
        this.nombreProducto = nombreProducto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }
}