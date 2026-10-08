package cl.nicolet.backend.controller;

import cl.nicolet.backend.dto.ServicioCreateDTO;
import cl.nicolet.backend.dto.ServicioDTO;
import cl.nicolet.backend.service.ServicioService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ServicioController.class)
class ServicioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ServicioService servicioService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/v2/nicolet/servicios - debe retornar HTTP 200 y lista de servicios")
    void debeListarServicios() throws Exception {
        ServicioDTO dto = new ServicioDTO(1L, "Corte", "Desc", 45, new BigDecimal("15000"), true);
        when(servicioService.findAll()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v2/nicolet/servicios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].nombre").value("Corte"));
    }

    @Test
    @DisplayName("GET /api/v2/nicolet/servicios/{id} - debe retornar HTTP 200 y servicio")
    void debeObtenerServicioPorId() throws Exception {
        ServicioDTO dto = new ServicioDTO(2L, "Manicura", "Desc", 60, new BigDecimal("20000"), true);
        when(servicioService.findById(2L)).thenReturn(dto);

        mockMvc.perform(get("/api/v2/nicolet/servicios/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2L))
                .andExpect(jsonPath("$.nombre").value("Manicura"));
    }

    @Test
    @DisplayName("POST /api/v2/nicolet/servicios - debe retornar HTTP 201 Created")
    void debeCrearServicio() throws Exception {
        ServicioCreateDTO createDTO = new ServicioCreateDTO("Facial", "Desc", 45, new BigDecimal("25000"));
        ServicioDTO responseDTO = new ServicioDTO(3L, "Facial", "Desc", 45, new BigDecimal("25000"), true);

        when(servicioService.crear(any(ServicioCreateDTO.class))).thenReturn(responseDTO);

        mockMvc.perform(post("/api/v2/nicolet/servicios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(3L))
                .andExpect(jsonPath("$.nombre").value("Facial"));
    }

    @Test
    @DisplayName("DELETE /api/v2/nicolet/servicios/{id} - debe retornar HTTP 204 No Content")
    void debeDesactivarServicio() throws Exception {
        mockMvc.perform(delete("/api/v2/nicolet/servicios/1"))
                .andExpect(status().isNoContent());
    }
}
