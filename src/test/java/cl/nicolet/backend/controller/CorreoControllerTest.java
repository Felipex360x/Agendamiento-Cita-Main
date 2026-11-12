package cl.nicolet.backend.controller;

import cl.nicolet.backend.model.Usuario;
import cl.nicolet.backend.repository.TipoUsuarioRepository;
import cl.nicolet.backend.repository.UsuarioRepository;
import cl.nicolet.backend.security.JwtUtils;
import cl.nicolet.backend.security.UserDetailsImpl;
import cl.nicolet.backend.service.EmailService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Los endpoints de correos exponen la bandeja (datos de clientes) y permiten enviar correo
 * desde la cuenta del negocio: solo un administrador puede usarlos.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CorreoControllerTest {

    private static final String BASE = "/api/v2/nicolet/correos";
    private static final String FLUJO_VALIDO = """
            {"correoCliente":"clienta@correo.test","nombreCliente":"Ana","apellidoCliente":"Rojas",
             "correoTrabajador":"pro@correo.test","nombreTrabajador":"Camila","apellidoTrabajador":"Silva"}""";

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private TipoUsuarioRepository tipoUsuarioRepository;
    @Autowired private EmailService emailService;

    private Usuario admin, cliente, profesional;
    private String hostOriginal, remitenteOriginal;
    private int puertoOriginal;

    @BeforeEach
    void fixtures() {
        String sufijo = UUID.randomUUID().toString().substring(0, 8);
        admin = usuario("admin", "ADMINISTRADOR", sufijo);
        cliente = usuario("cliente", "CLIENTE", sufijo);
        profesional = usuario("pro", "PROFESIONAL", sufijo);
        hostOriginal = emailService.getMailHost();
        puertoOriginal = emailService.getMailPort();
        remitenteOriginal = emailService.getMailUsername();
        emailService.limpiarBandeja();
    }

    @AfterEach
    void limpiar() {
        emailService.configurarCredencialesSmtp(remitenteOriginal, "");
        emailService.limpiarBandeja();
        usuarioRepository.deleteAll(List.of(admin, cliente, profesional));
    }

    private Usuario usuario(String alias, String rol, String sufijo) {
        Usuario u = new Usuario();
        u.setNombre(alias);
        u.setApellidoP("Correos");
        u.setCorreo(alias + "." + sufijo + "@correos.test");
        u.setPassword(passwordEncoder.encode("Clave-Segura-123"));
        u.setTipoUsuario(tipoUsuarioRepository.findByNombreIgnoreCase(rol).orElseThrow());
        return usuarioRepository.save(u);
    }

    private ResultActions como(Usuario u, MockHttpServletRequestBuilder req) throws Exception {
        if (u != null) {
            req.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtUtils.generarToken(UserDetailsImpl.build(u)));
        }
        return mockMvc.perform(req);
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder req, String cuerpo) {
        return req.contentType(MediaType.APPLICATION_JSON).content(cuerpo);
    }

    @Test
    @DisplayName("Todos los endpoints de correos: anónimo 401, cliente y profesional 403")
    void soloAdministrador() throws Exception {
        String smtp = "{\"remitente\":\"otro@correo.test\",\"appPassword\":\"abcd\"}";
        for (Usuario noAdmin : new Usuario[]{null, cliente, profesional}) {
            var esperado = noAdmin == null ? status().isUnauthorized() : status().isForbidden();
            como(noAdmin, get(BASE)).andExpect(esperado);
            como(noAdmin, delete(BASE)).andExpect(esperado);
            como(noAdmin, json(post(BASE + "/configurar-smtp"), smtp)).andExpect(esperado);
            como(noAdmin, json(post(BASE + "/probar-flujo"), FLUJO_VALIDO)).andExpect(esperado);
        }
        // Nada de lo anterior debe haber generado correos ni cambiado el remitente.
        assertThat(emailService.listarCorreosEnviados()).isEmpty();
        assertThat(emailService.getMailUsername()).isEqualTo(remitenteOriginal);
    }

    @Test
    @DisplayName("El administrador ve la bandeja, ejecuta el flujo de prueba y la vacía")
    void administradorOperaLaBandeja() throws Exception {
        como(admin, json(post(BASE + "/probar-flujo"), FLUJO_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enviados.length()").value(6))
                .andExpect(jsonPath("$.correoCliente").value("clienta@correo.test"));
        como(admin, get(BASE)).andExpect(status().isOk()).andExpect(jsonPath("$.correos.length()").value(6));
        como(admin, delete(BASE)).andExpect(status().isOk()).andExpect(jsonPath("$.correos.length()").value(0));
    }

    @Test
    @DisplayName("configurar-smtp no permite cambiar el servidor ni el puerto")
    void configurarSmtpIgnoraHostYPuerto() throws Exception {
        String cuerpo = "{\"host\":\"smtp.atacante.test\",\"port\":2525,"
                + "\"remitente\":\"nuevo@correo.test\",\"appPassword\":\"\"}";
        como(admin, json(post(BASE + "/configurar-smtp"), cuerpo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.remitente").value("nuevo@correo.test"));
        assertThat(emailService.getMailHost()).isEqualTo(hostOriginal);
        assertThat(emailService.getMailPort()).isEqualTo(puertoOriginal);
    }

    @Test
    @DisplayName("Los cuerpos inválidos se rechazan con 400 y no generan correos")
    void validaLosDatos() throws Exception {
        como(admin, json(post(BASE + "/configurar-smtp"), "{\"remitente\":\"no-es-correo\"}"))
                .andExpect(status().isBadRequest());
        como(admin, json(post(BASE + "/probar-flujo"), "{}")).andExpect(status().isBadRequest());
        como(admin, json(post(BASE + "/probar-flujo"), FLUJO_VALIDO.replace("clienta@correo.test", "sin-arroba")))
                .andExpect(status().isBadRequest());
        // Un salto de línea en el nombre terminaría en el asunto del correo.
        como(admin, json(post(BASE + "/probar-flujo"), FLUJO_VALIDO.replace("\"Ana\"", "\"Ana\\r\\nBcc: x@y.test\"")))
                .andExpect(status().isBadRequest());
        como(admin, json(post(BASE + "/probar-flujo"), FLUJO_VALIDO.replace("\"Ana\"", "\"" + "a".repeat(101) + "\"")))
                .andExpect(status().isBadRequest());
        assertThat(emailService.listarCorreosEnviados()).isEmpty();
    }

    @Test
    @DisplayName("La bandeja en memoria tiene un tope y conserva los correos más recientes")
    void bandejaAcotada() throws Exception {
        for (int i = 0; i < 40; i++) { // 40 × 6 = 240 correos
            como(admin, json(post(BASE + "/probar-flujo"), FLUJO_VALIDO)).andExpect(status().isOk());
        }
        var bandeja = emailService.listarCorreosEnviados();
        assertThat(bandeja).hasSize(200);
        assertThat(bandeja.get(0).getId()).isGreaterThan(bandeja.get(199).getId());
    }
}
