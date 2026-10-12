package cl.nicolet.backend.controller;

import cl.nicolet.backend.dto.BloqueHorarioDTO;
import cl.nicolet.backend.service.DisponibilidadService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DisponibilidadController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "cl\\.nicolet\\.backend\\.security\\..*"))
@AutoConfigureMockMvc(addFilters = false)
class DisponibilidadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DisponibilidadService disponibilidadService;

    @Test
    @DisplayName("GET /api/v2/nicolet/disponibilidad - debe retornar HTTP 200 y lista de bloques")
    void debeRetornarDisponibilidadExitosamente() throws Exception {
        BloqueHorarioDTO b1 = new BloqueHorarioDTO(LocalTime.of(10, 0), LocalTime.of(10, 45), true, null);
        BloqueHorarioDTO b2 = new BloqueHorarioDTO(LocalTime.of(10, 30), LocalTime.of(11, 15), false, "OCUPADO");

        LocalDate fecha = LocalDate.of(2026, 10, 20);

        when(disponibilidadService.calcularDisponibilidad(eq(1L), eq(fecha), eq(2L)))
                .thenReturn(List.of(b1, b2));

        mockMvc.perform(get("/api/v2/nicolet/disponibilidad")
                        .param("trabajadorId", "1")
                        .param("fecha", "2026-10-20")
                        .param("servicioId", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].horaInicio").value("10:00:00"))
                .andExpect(jsonPath("$[0].disponible").value(true))
                .andExpect(jsonPath("$[1].disponible").value(false))
                .andExpect(jsonPath("$[1].motivoNoDisponible").value("OCUPADO"));
    }

    @Test
    @DisplayName("GET /api/v2/nicolet/disponibilidad - debe fallar con 400 si faltan parametros obligatorios")
    void debeFallarSiFaltaParametroObligatorio() throws Exception {
        mockMvc.perform(get("/api/v2/nicolet/disponibilidad")
                        .param("trabajadorId", "1"))
                .andExpect(status().isBadRequest());
    }
}
