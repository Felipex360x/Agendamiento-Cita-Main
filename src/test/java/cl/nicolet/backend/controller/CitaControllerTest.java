package cl.nicolet.backend.controller;

import cl.nicolet.backend.dto.CambioEstadoCitaDTO;
import cl.nicolet.backend.dto.CitaCreateDTO;
import cl.nicolet.backend.dto.CitaDTO;
import cl.nicolet.backend.model.EstadoCita;
import cl.nicolet.backend.service.CitaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@WebMvcTest(controllers = CitaController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = "cl\\.nicolet\\.backend\\.security\\..*"))
@AutoConfigureMockMvc(addFilters = false)
class CitaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CitaService citaService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/v2/nicolet/citas - debe retornar HTTP 200 y lista de citas")
    void debeListarCitas() throws Exception {
        CitaDTO dto = new CitaDTO();
        dto.setId(1L);
        dto.setCodigoReserva("RES-2026-0001");
        dto.setEstado(EstadoCita.CONFIRMADA);
        dto.setPrecioFinal(new BigDecimal("18000"));

        when(citaService.findAll()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v2/nicolet/citas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigoReserva").value("RES-2026-0001"))
                .andExpect(jsonPath("$[0].estado").value("CONFIRMADA"));
    }

    @Test
    @DisplayName("GET /api/v2/nicolet/citas/codigo/{codigo} - debe retornar HTTP 200")
    void debeBuscarPorCodigo() throws Exception {
        CitaDTO dto = new CitaDTO();
        dto.setId(5L);
        dto.setCodigoReserva("RES-TEST");

        when(citaService.findByCodigo("RES-TEST")).thenReturn(dto);

        mockMvc.perform(get("/api/v2/nicolet/citas/codigo/RES-TEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigoReserva").value("RES-TEST"));
    }

    @Test
    @DisplayName("POST /api/v2/nicolet/citas - debe agendar cita y retornar HTTP 201")
    void debeAgendarCita() throws Exception {
        LocalDateTime fecha = LocalDateTime.now().plusDays(2);
        CitaCreateDTO req = new CitaCreateDTO(1L, 2L, 3L, fecha, "Notas");

        CitaDTO res = new CitaDTO();
        res.setId(10L);
        res.setCodigoReserva("RES-NUEVA");
        res.setEstado(EstadoCita.CONFIRMADA);

        when(citaService.agendar(any(CitaCreateDTO.class))).thenReturn(res);

        mockMvc.perform(post("/api/v2/nicolet/citas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L))
                .andExpect(jsonPath("$.codigoReserva").value("RES-NUEVA"));
    }

    @Test
    @DisplayName("PATCH /api/v2/nicolet/citas/{id}/estado - debe cambiar estado y retornar HTTP 200")
    void debeCambiarEstadoCita() throws Exception {
        CambioEstadoCitaDTO req = new CambioEstadoCitaDTO(EstadoCita.COMPLETADA, "Servicio realizado");

        CitaDTO res = new CitaDTO();
        res.setId(1L);
        res.setEstado(EstadoCita.COMPLETADA);

        when(citaService.cambiarEstado(eq(1L), any(CambioEstadoCitaDTO.class))).thenReturn(res);

        mockMvc.perform(patch("/api/v2/nicolet/citas/1/estado")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADA"));
    }
}
