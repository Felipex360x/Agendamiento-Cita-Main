package cl.nicolet.backend.security;

import cl.nicolet.backend.model.Usuario;
import cl.nicolet.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** E14 (límites de entrada alineados con la BD) y E22 (contrato de errores) con la seguridad real. */
@SpringBootTest
@AutoConfigureMockMvc
class ValidacionApiIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private UsuarioRepository usuarioRepository;

    private String admin() {
        Usuario a = usuarioRepository.findByCorreoIgnoreCase("maximo.rojas@estudio.cl").orElseThrow();
        return "Bearer " + jwtUtils.generarToken(UserDetailsImpl.build(a));
    }

    private ResultActions enviar(String url, String json) throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url).header(HttpHeaders.AUTHORIZATION, admin())
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static String servicio(String nombre, int duracion, String precio) {
        return "{\"nombre\":\"" + nombre + "\",\"duracionMinutos\":" + duracion + ",\"precio\":" + precio + "}";
    }

    private static String trabajador(String comision) {
        return "{\"usuarioId\":1,\"cargoEspecialidad\":\"Estilista\",\"comisionPorcentaje\":" + comision + "}";
    }

    @Test
    @DisplayName("E22: un 400 de validación incluye codigo, mensaje, error, errores y los campos al nivel superior")
    void contratoDeValidacion() throws Exception {
        enviar("/api/v2/nicolet/servicios", servicio("Corte", 5, "10000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.mensaje").value("La duración mínima es de 10 minutos"))
                .andExpect(jsonPath("$.error").value("La duración mínima es de 10 minutos"))
                .andExpect(jsonPath("$.errores.duracionMinutos").value("La duración mínima es de 10 minutos"))
                .andExpect(jsonPath("$.duracionMinutos").value("La duración mínima es de 10 minutos"));
    }

    @Test
    @DisplayName("E14: servicios — duración máxima, precio con precisión de DECIMAL(10,2) y nombre ≤ 120")
    void limitesDeServicio() throws Exception {
        enviar("/api/v2/nicolet/servicios", servicio("Corte", 1441, "10000")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.duracionMinutos").exists());
        enviar("/api/v2/nicolet/servicios", servicio("Corte", 30, "100000000.00")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.precio").exists());
        enviar("/api/v2/nicolet/servicios", servicio("Corte", 30, "10.123")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.precio").exists());
        enviar("/api/v2/nicolet/servicios", servicio("N".repeat(121), 30, "10000")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists());
        enviar("/api/v2/nicolet/servicios", servicio("Corte", 30, "0")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.precio").exists());
    }

    @Test
    @DisplayName("E14: comisión fuera de 0..100 o con más de 2 decimales → 400; los extremos 0 y 100 son válidos")
    void limitesDeComision() throws Exception {
        for (String invalida : new String[]{"-0.01", "100.01", "101", "12.345", "1000"}) {
            enviar("/api/v2/nicolet/trabajadores", trabajador(invalida)).andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errores.comisionPorcentaje").exists());
        }
        // 0 y 100 pasan la validación de entrada (después el usuario inexistente/duplicado puede dar 404/409, nunca 400 por comisión)
        for (String valida : new String[]{"0", "100", "37.5"}) {
            enviar("/api/v2/nicolet/trabajadores", trabajador(valida)).andExpect(
                    org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.errores.comisionPorcentaje").doesNotExist());
        }
    }

    @Test
    @DisplayName("E14: rol — nombre ≤ 50; cliente — teléfono y RUT ≤ 20")
    void otrosLimites() throws Exception {
        enviar("/api/v2/nicolet/tipos-usuario", "{\"nombre\":\"" + "R".repeat(51) + "\"}").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.nombre").exists());
        enviar("/api/v2/nicolet/clientes", "{\"usuarioId\":1,\"telefono\":\"" + "9".repeat(21) + "\",\"rutDni\":\"" + "1".repeat(21) + "\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.telefono").exists())
                .andExpect(jsonPath("$.errores.rutDni").exists());
    }

    @Test
    @DisplayName("E22: JSON malformado y conflicto devuelven el contrato con codigo y mensaje")
    void otrosErrores() throws Exception {
        enviar("/api/v2/nicolet/servicios", "{no es json").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"))
                .andExpect(jsonPath("$.mensaje").exists());
        // correo ya registrado por el flujo de alta administrativa → 400 de negocio con contrato
        enviar("/api/v2/nicolet/usuarios", "{\"nombre\":\"A\",\"apellidoP\":\"B\",\"correo\":\"maximo.rojas@estudio.cl\",\"password\":\"Clave-1234\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("ya se encuentra registrado")));
    }
}
