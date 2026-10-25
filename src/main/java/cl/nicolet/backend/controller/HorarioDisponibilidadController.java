package cl.nicolet.backend.controller;

import cl.nicolet.backend.dto.HorarioDisponibilidadDTO;
import cl.nicolet.backend.service.HorarioDisponibilidadService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v2/nicolet/horarios")
public class HorarioDisponibilidadController {
    private final HorarioDisponibilidadService horarioService;

    public HorarioDisponibilidadController(HorarioDisponibilidadService horarioService) {
        this.horarioService = horarioService;
    }

    @GetMapping("/trabajador/{trabajadorId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or @trabajadorService.esPropietario(#p0, principal.id)")
    public ResponseEntity<List<HorarioDisponibilidadDTO>> listar(@PathVariable Long trabajadorId) {
        return ResponseEntity.ok(horarioService.listar(trabajadorId));
    }

    @PutMapping("/trabajador/{trabajadorId}")
    @PreAuthorize("hasRole('ADMINISTRADOR') or @trabajadorService.esPropietario(#p0, principal.id)")
    public ResponseEntity<List<HorarioDisponibilidadDTO>> actualizar(@PathVariable Long trabajadorId,
                                                                      @RequestBody List<HorarioDisponibilidadDTO> horarios) {
        return ResponseEntity.ok(horarioService.actualizar(trabajadorId, horarios));
    }
}
