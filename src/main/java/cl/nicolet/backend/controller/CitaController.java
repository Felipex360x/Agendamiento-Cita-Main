package cl.nicolet.backend.controller;

import cl.nicolet.backend.dto.CambioEstadoCitaDTO;
import cl.nicolet.backend.dto.CitaCreateDTO;
import cl.nicolet.backend.dto.CitaDTO;
import cl.nicolet.backend.service.CitaService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Citas", description = "Operaciones del núcleo de agendamiento de citas")
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v2/nicolet/citas")
public class CitaController {

    @Autowired
    private CitaService citaService;

    @Operation(summary = "Listar todas las Citas registradas")
    @ApiResponse(responseCode = "200", description = "Lista de citas obtenida")
    @GetMapping
    public ResponseEntity<List<CitaDTO>> getAll() {
        return ResponseEntity.ok(citaService.findAll());
    }

    @Operation(summary = "Buscar Cita por ID")
    @GetMapping("/{id}")
    public ResponseEntity<CitaDTO> getById(@Parameter(description = "ID de la cita") @PathVariable Long id) {
        return ResponseEntity.ok(citaService.findById(id));
    }

    @Operation(summary = "Buscar Cita por Código de Reserva")
    @GetMapping("/codigo/{codigoReserva}")
    public ResponseEntity<CitaDTO> getByCodigo(@Parameter(description = "Código de reserva único (ej: RES-2026-XXXX)") @PathVariable String codigoReserva) {
        return ResponseEntity.ok(citaService.findByCodigo(codigoReserva));
    }

    @Operation(summary = "Listar Citas de un Trabajador")
    @GetMapping("/trabajador/{trabajadorId}")
    public ResponseEntity<List<CitaDTO>> getByTrabajador(@PathVariable Long trabajadorId) {
        return ResponseEntity.ok(citaService.findByTrabajador(trabajadorId));
    }

    @Operation(summary = "Listar Citas de un Cliente")
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<List<CitaDTO>> getByCliente(@PathVariable Long clienteId) {
        return ResponseEntity.ok(citaService.findByCliente(clienteId));
    }

    @Operation(summary = "Agendar una nueva Cita")
    @ApiResponse(responseCode = "201", description = "Cita agendada exitosamente")
    @PostMapping
    public ResponseEntity<CitaDTO> agendar(@Valid @RequestBody CitaCreateDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(citaService.agendar(dto));
    }

    @Operation(summary = "Cambiar Estado de una Cita (Confirmar, Cancelar, etc.)")
    @PatchMapping("/{id}/estado")
    public ResponseEntity<CitaDTO> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambioEstadoCitaDTO dto
    ) {
        return ResponseEntity.ok(citaService.cambiarEstado(id, dto));
    }
}
