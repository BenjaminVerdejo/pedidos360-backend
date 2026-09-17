package pedidos_service.controller;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pedidos_service.dto.ItemPedidoDTO;
import pedidos_service.dto.PedidoDTO;
import pedidos_service.entity.Cliente;
import pedidos_service.entity.EstadoPedido;
import pedidos_service.entity.ItemPedido;
import pedidos_service.entity.Pedido;
import pedidos_service.repository.ClienteRepository;
import pedidos_service.repository.PedidoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;

    public PedidoController(PedidoRepository pedidoRepository, ClienteRepository clienteRepository) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
    }

    @GetMapping
    public List<PedidoDTO> listar() {
        return pedidoRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoDTO> obtener(@PathVariable Long id) {
        return pedidoRepository.findById(id)
                .map(pedido -> ResponseEntity.ok(toDTO(pedido)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<PedidoDTO> crear(@RequestBody PedidoDTO dto) {
        Cliente cliente = clienteRepository.findById(dto.getClienteId())
                .orElseThrow(() -> new EntityNotFoundException("Cliente no encontrado: " + dto.getClienteId()));

        Pedido pedido = new Pedido(LocalDateTime.now(), EstadoPedido.PENDIENTE, cliente);

        if (dto.getItems() != null) {
            for (ItemPedidoDTO itemDTO : dto.getItems()) {
                ItemPedido item = new ItemPedido(
                        itemDTO.getNombreProducto(),
                        itemDTO.getCantidad(),
                        itemDTO.getPrecioUnitario(),
                        pedido
                );
                pedido.getItems().add(item);
            }
        }

        Pedido guardado = pedidoRepository.save(pedido);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(guardado));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<PedidoDTO> cambiarEstado(@PathVariable Long id, @RequestParam EstadoPedido nuevoEstado) {
        return pedidoRepository.findById(id)
                .map(pedido -> {
                    pedido.setEstado(nuevoEstado);
                    Pedido actualizado = pedidoRepository.save(pedido);
                    return ResponseEntity.ok(toDTO(actualizado));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (!pedidoRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        pedidoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private PedidoDTO toDTO(Pedido p) {
        List<ItemPedidoDTO> items = p.getItems().stream()
                .map(i -> new ItemPedidoDTO(i.getId(), i.getNombreProducto(), i.getCantidad(), i.getPrecioUnitario()))
                .collect(Collectors.toList());
        return new PedidoDTO(p.getId(), p.getFecha(), p.getEstado(), p.getCliente().getId(), items);
    }
}