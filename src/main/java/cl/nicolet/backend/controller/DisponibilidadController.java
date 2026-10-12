package cl.nicolet.backend.controller;

import cl.nicolet.backend.dto.BloqueHorarioDTO;
import cl.nicolet.backend.service.DisponibilidadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Disponibilidad", description = "Consulta pública de bloques horarios y disponibilidad de la agenda")
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/v2/nicolet/disponibilidad")
public class DisponibilidadController {

    @Autowired
    private DisponibilidadService disponibilidadService;

    @Operation(summary = "Consultar bloques horarios disponibles de un profesional en una fecha")
    @ApiResponse(responseCode = "200", description = "Lista de bloques horarios calculados con disponibilidad")
    @GetMapping
    public ResponseEntity<List<BloqueHorarioDTO>> getDisponibilidad(
            @Parameter(description = "ID único del trabajador/profesional", example = "1", required = true)
            @RequestParam Long trabajadorId,

            @Parameter(description = "Fecha de consulta en formato ISO (YYYY-MM-DD)", example = "2026-10-15", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,

            @Parameter(description = "ID del servicio a agendar (para calcular duración exacta en minutos)", example = "2")
            @RequestParam(required = false) Long servicioId
    ) {
        return ResponseEntity.ok(disponibilidadService.calcularDisponibilidad(trabajadorId, fecha, servicioId));
    }
}
