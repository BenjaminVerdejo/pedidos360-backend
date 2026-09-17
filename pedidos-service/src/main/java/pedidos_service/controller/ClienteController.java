package pedidos_service.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pedidos_service.dto.ClienteDTO;
import pedidos_service.entity.Cliente;
import pedidos_service.repository.ClienteRepository;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteRepository clienteRepository;

    public ClienteController(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @GetMapping
    public List<ClienteDTO> listar() {
        return clienteRepository.findAll()
                .stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteDTO> obtener(@PathVariable Long id) {
        return clienteRepository.findById(id)
                .map(cliente -> ResponseEntity.ok(toDTO(cliente)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ClienteDTO> crear(@Valid @RequestBody ClienteDTO dto) {
        Cliente cliente = new Cliente(dto.getNombre(), dto.getEmail(), dto.getTelefono(), dto.getDireccion());
        Cliente guardado = clienteRepository.save(cliente);
        return ResponseEntity.status(HttpStatus.CREATED).body(toDTO(guardado));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteDTO> actualizar(@PathVariable Long id, @Valid @RequestBody ClienteDTO dto) {
        return clienteRepository.findById(id)
                .map(cliente -> {
                    cliente.setNombre(dto.getNombre());
                    cliente.setEmail(dto.getEmail());
                    cliente.setTelefono(dto.getTelefono());
                    cliente.setDireccion(dto.getDireccion());
                    Cliente actualizado = clienteRepository.save(cliente);
                    return ResponseEntity.ok(toDTO(actualizado));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        if (!clienteRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        clienteRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ClienteDTO toDTO(Cliente c) {
        return new ClienteDTO(c.getId(), c.getNombre(), c.getEmail(), c.getTelefono(), c.getDireccion());
    }
}