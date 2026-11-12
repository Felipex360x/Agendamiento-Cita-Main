package cl.nicolet.backend.controller;

import cl.nicolet.backend.dto.MotivoDesactivacionDTO;
import cl.nicolet.backend.dto.UsuarioCreateDTO;
import cl.nicolet.backend.dto.UsuarioDTO;
import cl.nicolet.backend.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Usuarios", description = "Operaciones de gestión de usuarios")
@RestController
@PreAuthorize("hasRole('ADMINISTRADOR')")
@RequestMapping("/api/v2/nicolet/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioService usuarioService;

    @Operation(
        summary = "Listar todos los usuarios",
        description = "Retorna la lista completa de usuarios registrados en el sistema."
    )
    @ApiResponse(responseCode = "200", description = "Lista obtenida exitosamente")
    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> getAll() {
        return ResponseEntity.ok(usuarioService.findAll());
    }

    @Operation(summary = "Buscar usuario por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> getById(
            @Parameter(description = "ID único del usuario", required = true) @PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.findById(id));
    }

    @Operation(summary = "Registrar nuevo usuario")
    @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente")
    @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    @PostMapping
    public ResponseEntity<UsuarioDTO> crear(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Datos del nuevo usuario")
            @Valid @RequestBody UsuarioCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(dto));
    }

    @Operation(summary = "Actualizar usuario existente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Actualización exitosa"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    })
    @PutMapping("/{id}")
    public ResponseEntity<UsuarioDTO> actualizar(
            @Parameter(description = "ID del usuario a actualizar", required = true) @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Nuevos datos del usuario")
            @Valid @RequestBody UsuarioCreateDTO dto) {
        return ResponseEntity.ok(usuarioService.actualizar(id, dto));
    }

    @Operation(summary = "Desactivar usuario")
    @ApiResponse(responseCode = "200", description = "Usuario desactivado")
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    @PatchMapping("/{id}/desactivar")
    public ResponseEntity<UsuarioDTO> desactivar(
            @Parameter(description = "ID del usuario a desactivar", required = true) @PathVariable Long id,
            @Valid @RequestBody MotivoDesactivacionDTO dto) {
        return ResponseEntity.ok(usuarioService.desactivar(id, dto.getMotivo()));
    }

    @Operation(summary = "Reactivar usuario")
    @ApiResponse(responseCode = "200", description = "Usuario reactivado")
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    @PatchMapping("/{id}/activar")
    public ResponseEntity<UsuarioDTO> activar(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.activar(id));
    }
}
