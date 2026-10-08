package cl.nicolet.backend.controller;

import cl.nicolet.backend.dto.ClienteCreateDTO;
import cl.nicolet.backend.dto.ClienteDTO;
import cl.nicolet.backend.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Clientes", description = "Operaciones de gestión del perfil de Clientes")
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v2/nicolet/clientes")
public class ClienteController {

    @Autowired
    private ClienteService clienteService;

    @Operation(summary = "Listar todos los Clientes")
    @ApiResponse(responseCode = "200", description = "Lista de clientes obtenida exitosamente")
    @GetMapping
    public ResponseEntity<List<ClienteDTO>> getAll() {
        return ResponseEntity.ok(clienteService.findAll());
    }

    @Operation(summary = "Buscar Cliente por ID")
    @GetMapping("/{id}")
    public ResponseEntity<ClienteDTO> getById(@Parameter(description = "ID del cliente") @PathVariable Long id) {
        return ResponseEntity.ok(clienteService.findById(id));
    }

    @Operation(summary = "Buscar Cliente por ID de Usuario")
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<ClienteDTO> getByUsuarioId(@Parameter(description = "ID del usuario asociado") @PathVariable Long usuarioId) {
        return ResponseEntity.ok(clienteService.findByUsuarioId(usuarioId));
    }

    @Operation(summary = "Crear Perfil de Cliente")
    @ApiResponse(responseCode = "201", description = "Perfil de cliente creado")
    @PostMapping
    public ResponseEntity<ClienteDTO> crear(@Valid @RequestBody ClienteCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.crear(dto));
    }

    @Operation(summary = "Actualizar Perfil de Cliente")
    @PutMapping("/{id}")
    public ResponseEntity<ClienteDTO> actualizar(@PathVariable Long id, @Valid @RequestBody ClienteCreateDTO dto) {
        return ResponseEntity.ok(clienteService.actualizar(id, dto));
    }
}
