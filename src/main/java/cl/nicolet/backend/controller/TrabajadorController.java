package cl.nicolet.backend.controller;

import cl.nicolet.backend.dto.TrabajadorCreateDTO;
import cl.nicolet.backend.dto.TrabajadorDTO;
import cl.nicolet.backend.service.TrabajadorService;
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

@Tag(name = "Trabajadores", description = "Operaciones de gestión del personal / profesionales")
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v2/nicolet/trabajadores")
public class TrabajadorController {

    @Autowired
    private TrabajadorService trabajadorService;

    @Operation(summary = "Listar todos los Trabajadores activos")
    @ApiResponse(responseCode = "200", description = "Lista de trabajadores obtenida")
    @GetMapping
    public ResponseEntity<List<TrabajadorDTO>> getAll() {
        return ResponseEntity.ok(trabajadorService.findAll());
    }

    @Operation(summary = "Buscar Trabajador por ID")
    @GetMapping("/{id}")
    public ResponseEntity<TrabajadorDTO> getById(@Parameter(description = "ID del trabajador") @PathVariable Long id) {
        return ResponseEntity.ok(trabajadorService.findById(id));
    }

    @Operation(summary = "Buscar Trabajador por ID de Usuario")
    @GetMapping("/usuario/{usuarioId}")
    public ResponseEntity<TrabajadorDTO> getByUsuarioId(@Parameter(description = "ID del usuario asociado") @PathVariable Long usuarioId) {
        return ResponseEntity.ok(trabajadorService.findByUsuarioId(usuarioId));
    }

    @Operation(summary = "Crear Perfil de Trabajador")
    @ApiResponse(responseCode = "201", description = "Perfil de trabajador creado")
    @PostMapping
    public ResponseEntity<TrabajadorDTO> crear(@Valid @RequestBody TrabajadorCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(trabajadorService.crear(dto));
    }

    @Operation(summary = "Actualizar Perfil de Trabajador")
    @PutMapping("/{id}")
    public ResponseEntity<TrabajadorDTO> actualizar(@PathVariable Long id, @Valid @RequestBody TrabajadorCreateDTO dto) {
        return ResponseEntity.ok(trabajadorService.actualizar(id, dto));
    }
}
