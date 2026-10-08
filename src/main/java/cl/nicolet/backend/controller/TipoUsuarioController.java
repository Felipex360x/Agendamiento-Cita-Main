package cl.nicolet.backend.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.nicolet.backend.dto.TipoUsuarioCreateDTO;
import cl.nicolet.backend.dto.TipoUsuarioDTO;
import cl.nicolet.backend.service.TipoUsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Tipos de Usuario", description = "Operaciones de gestión de roles y tipos de usuario")
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v2/nicolet/tipos-usuario")
public class TipoUsuarioController {

    @Autowired
    private TipoUsuarioService tipoUsuarioService;

    @Operation(summary = "Listar todos los tipos de usuario", description = "Retorna la lista de roles registrados en el sistema")
    @ApiResponse(responseCode = "200", description = "Lista obtenida exitosamente")
    @GetMapping
    public ResponseEntity<List<TipoUsuarioDTO>> getAll() {
        return ResponseEntity.ok(tipoUsuarioService.findAll());
    }

    @Operation(summary = "Buscar tipo de usuario por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tipo de usuario encontrado"),
        @ApiResponse(responseCode = "404", description = "Tipo de usuario no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<TipoUsuarioDTO> getById(
            @Parameter(description = "ID único del tipo de usuario", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(tipoUsuarioService.findById(id));
    }

    @Operation(summary = "Crear nuevo tipo de usuario")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Tipo de usuario creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    })
    @PostMapping
    public ResponseEntity<TipoUsuarioDTO> crear(@Valid @RequestBody TipoUsuarioCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tipoUsuarioService.crear(dto));
    }

    @Operation(summary = "Actualizar tipo de usuario")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tipo de usuario actualizado"),
        @ApiResponse(responseCode = "404", description = "Tipo de usuario no encontrado"),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    })
    @PutMapping("/{id}")
    public ResponseEntity<TipoUsuarioDTO> actualizar(
            @Parameter(description = "ID del tipo de usuario a actualizar", required = true)
            @PathVariable Long id,
            @Valid @RequestBody TipoUsuarioCreateDTO dto) {
        return ResponseEntity.ok(tipoUsuarioService.actualizar(id, dto));
    }

    @Operation(summary = "Eliminar tipo de usuario")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Tipo de usuario eliminado"),
        @ApiResponse(responseCode = "404", description = "Tipo de usuario no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "ID del tipo de usuario a eliminar", required = true)
            @PathVariable Long id) {
        tipoUsuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

}
