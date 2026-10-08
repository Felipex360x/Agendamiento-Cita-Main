package cl.nicolet.backend.controller;

import cl.nicolet.backend.dto.ServicioCreateDTO;
import cl.nicolet.backend.dto.ServicioDTO;
import cl.nicolet.backend.service.ServicioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

import java.util.List;

@Tag(name = "Servicios", description = "Operaciones de catálogo de servicios ofrecidos")
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v2/nicolet/servicios")
public class ServicioController {

    @Autowired
    private ServicioService servicioService;

    @Operation(summary = "Listar todos los Servicios activos")
    @ApiResponse(responseCode = "200", description = "Lista de servicios obtenida")
    @GetMapping
    public ResponseEntity<List<ServicioDTO>> getAll() {
        return ResponseEntity.ok(servicioService.findAll());
    }

    @Operation(summary = "Buscar Servicio por ID")
    @GetMapping("/{id}")
    public ResponseEntity<ServicioDTO> getById(@Parameter(description = "ID del servicio") @PathVariable Long id) {
        return ResponseEntity.ok(servicioService.findById(id));
    }

    @Operation(summary = "Crear un nuevo Servicio")
    @ApiResponse(responseCode = "201", description = "Servicio creado exitosamente")
    @PostMapping
    public ResponseEntity<ServicioDTO> crear(@Valid @RequestBody ServicioCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicioService.crear(dto));
    }

    @Operation(summary = "Actualizar un Servicio")
    @PutMapping("/{id}")
    public ResponseEntity<ServicioDTO> actualizar(@PathVariable Long id, @Valid @RequestBody ServicioCreateDTO dto) {
        return ResponseEntity.ok(servicioService.actualizar(id, dto));
    }

    @Operation(summary = "Desactivar un Servicio (Baja lógica)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable Long id) {
        servicioService.desactivar(id);
        return ResponseEntity.noContent().build();
    }
}
