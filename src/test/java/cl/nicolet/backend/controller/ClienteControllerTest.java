package cl.nicolet.backend.controller;

import cl.nicolet.backend.dto.ClienteCreateDTO;
import cl.nicolet.backend.dto.ClienteDTO;
import cl.nicolet.backend.service.ClienteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ClienteController.class)
class ClienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClienteService clienteService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/v2/nicolet/clientes - debe retornar HTTP 200")
    void debeListarClientes() throws Exception {
        ClienteDTO dto = new ClienteDTO();
        dto.setId(1L);
        dto.setTelefono("+56912345678");

        when(clienteService.findAll()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/v2/nicolet/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].telefono").value("+56912345678"));
    }

    @Test
    @DisplayName("POST /api/v2/nicolet/clientes - debe crear cliente y retornar HTTP 201")
    void debeCrearCliente() throws Exception {
        ClienteCreateDTO req = new ClienteCreateDTO(2L, "+56912345678", "19876543-2", null, "Notas");
        ClienteDTO res = new ClienteDTO();
        res.setId(10L);
        res.setTelefono("+56912345678");

        when(clienteService.crear(any(ClienteCreateDTO.class))).thenReturn(res);

        mockMvc.perform(post("/api/v2/nicolet/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10L));
    }
}
