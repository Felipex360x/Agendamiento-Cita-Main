package cl.nicolet.backend.security;

import cl.nicolet.backend.dto.*;
import cl.nicolet.backend.model.EstadoCita;
import cl.nicolet.backend.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.SecretKey;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class QaStressAndSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private TipoUsuarioRepository tipoUsuarioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TrabajadorRepository trabajadorRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private CitaRepository citaRepository;

    private String adminToken;

    @BeforeEach
    void setUp() throws Exception {
        loginAttemptService.reset();
        LoginRequestDTO login = new LoginRequestDTO("admin.test@nicolet.cl", "AdminTest123!");
        MvcResult res = mockMvc.perform(post("/api/v2/nicolet/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();
        adminToken = objectMapper.readTree(res.getResponse().getContentAsString()).get("token").asText();
    }

    @Test
    @DisplayName("Crear, editar roles y eliminar usuarios con perfiles y citas")
    void testCicloCaoticoUsuariosRolesYEliminacionEnCascada() throws Exception {
        Long rolClienteId = tipoUsuarioRepository.findByNombreIgnoreCase("CLIENTE").orElseThrow().getId();
        Long rolProId = tipoUsuarioRepository.findByNombreIgnoreCase("PROFESIONAL").orElseThrow().getId();
        Long rolAdminId = tipoUsuarioRepository.findByNombreIgnoreCase("ADMINISTRADOR").orElseThrow().getId();

        List<Long> usuariosCreados = new ArrayList<>();

        for (int i = 1; i <= 10; i++) {
            Long rolId = (i % 3 == 0) ? rolAdminId : (i % 2 == 0 ? rolProId : rolClienteId);
            UsuarioCreateDTO dto = new UsuarioCreateDTO(
                    "StressUser" + i,
                    "Apellido" + i,
                    "  STRESS.USER." + i + "@NICOLET.CL  ",
                    "ClaveSegura#" + i,
                    rolId
            );

            MvcResult createRes = mockMvc.perform(post("/api/v2/nicolet/usuarios")
                            .header("Authorization", "Bearer " + adminToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(dto)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.correo").value("stress.user." + i + "@nicolet.cl"))
                    .andReturn();

            Long userId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();
            usuariosCreados.add(userId);

            if (rolId.equals(rolClienteId)) {
                assertThat(clienteRepository.findByUsuarioId(userId)).isPresent();
            } else if (rolId.equals(rolProId)) {
                assertThat(trabajadorRepository.findByUsuarioId(userId)).isPresent();
            }
        }

        // Cambiar rol de cliente a profesional manteniendo contraseña actual
        Long userMutanteId = usuariosCreados.get(0);
        UsuarioCreateDTO updateToPro = new UsuarioCreateDTO(
                "MutantePro",
                "Cambiado",
                "stress.user.1@nicolet.cl",
                "",
                rolProId
        );
        mockMvc.perform(put("/api/v2/nicolet/usuarios/" + userMutanteId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateToPro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("MutantePro"))
                .andExpect(jsonPath("$.tipoUsuario.nombre").value("PROFESIONAL"));

        assertThat(trabajadorRepository.findByUsuarioId(userMutanteId)).isPresent();
        assertThat(trabajadorRepository.findByUsuarioId(userMutanteId).get().getActivo()).isTrue();

        mockMvc.perform(post("/api/v2/nicolet/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDTO("stress.user.1@nicolet.cl", "ClaveSegura#1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("PROFESIONAL"));

        // Volver a rol cliente y actualizar contraseña
        UsuarioCreateDTO updateBackToClient = new UsuarioCreateDTO(
                "MutanteCliente",
                "ReCambiado",
                "stress.user.1@nicolet.cl",
                "NuevaClaveMutante#99",
                rolClienteId
        );
        mockMvc.perform(put("/api/v2/nicolet/usuarios/" + userMutanteId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateBackToClient)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoUsuario.nombre").value("CLIENTE"));

        assertThat(trabajadorRepository.findByUsuarioId(userMutanteId).get().getActivo()).isFalse();

        updateToPro.setPassword("NuevaClaveMutante#99");
        mockMvc.perform(put("/api/v2/nicolet/usuarios/" + userMutanteId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateToPro)))
                .andExpect(status().isOk());
        assertThat(trabajadorRepository.findByUsuarioId(userMutanteId).get().getActivo()).isTrue();

        Long clienteId = clienteRepository.findByUsuarioId(userMutanteId).orElseThrow().getId();
        Long trabajadorId = trabajadorRepository.findByUsuarioId(usuariosCreados.get(1)).orElseThrow().getId();
        Long servicioId = servicioRepository.findByActivoTrue().get(0).getId();

        LocalDate proximoMartes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.TUESDAY));
        CitaCreateDTO citaDto = new CitaCreateDTO(
                clienteId,
                trabajadorId,
                servicioId,
                proximoMartes.atTime(10, 0),
                "Cita de prueba"
        );

        mockMvc.perform(post("/api/v2/nicolet/citas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(citaDto)))
                .andExpect(status().isCreated());

        for (Long uId : usuariosCreados) {
            mockMvc.perform(delete("/api/v2/nicolet/usuarios/" + uId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());
            assertThat(usuarioRepository.findById(uId)).isEmpty();
        }
    }

    @Test
    @DisplayName("Validar correos duplicados y roles inexistentes en usuarios")
    void testCasosBordeUsuarios() throws Exception {
        UsuarioCreateDTO dupDto = new UsuarioCreateDTO(
                "Clon", "Rojas", "  MAXIMO.ROJAS@ESTUDIO.CL  ", "Clave12345", null
        );
        mockMvc.perform(post("/api/v2/nicolet/usuarios")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dupDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        mockMvc.perform(post("/api/v2/nicolet/usuarios/manual")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dupDto)))
                .andExpect(status().isBadRequest());

        UsuarioCreateDTO rolInexistenteDto = new UsuarioCreateDTO(
                "Fantasma", "Rol", "fantasma.rol@nicolet.cl", "Clave12345", 999999L
        );
        mockMvc.perform(post("/api/v2/nicolet/usuarios")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rolInexistenteDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());

        Long idMartina = usuarioRepository.findByCorreoIgnoreCase("martina.contreras@gmail.com").orElseThrow().getId();
        UsuarioCreateDTO colisionCorreo = new UsuarioCreateDTO(
                "Martina", "Contreras", "  CAMILA.SILVA@ESTUDIO.CL ", "123456", null
        );
        mockMvc.perform(put("/api/v2/nicolet/usuarios/" + idMartina)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(colisionCorreo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        UsuarioCreateDTO colisionRol = new UsuarioCreateDTO(
                "Martina", "Contreras", "martina.contreras@gmail.com", "", 888888L
        );
        mockMvc.perform(put("/api/v2/nicolet/usuarios/" + idMartina)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(colisionRol)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Validar creación, baja lógica, reactivación y límites de servicios")
    void testCicloServiciosYValidaciones() throws Exception {
        ServicioCreateDTO nuevo = new ServicioCreateDTO(
                "  Lifting de Pestañas QA  ",
                "Servicio de prueba",
                45,
                new BigDecimal("25000.00")
        );

        MvcResult createRes = mockMvc.perform(post("/api/v2/nicolet/servicios")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nuevo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nombre").value("Lifting de Pestañas QA"))
                .andExpect(jsonPath("$.activo").value(true))
                .andReturn();

        Long servicioId = objectMapper.readTree(createRes.getResponse().getContentAsString()).get("id").asLong();

        ServicioCreateDTO duplicado = new ServicioCreateDTO(
                "   LIFTING DE PESTAÑAS QA   ",
                "Intento duplicado",
                30,
                new BigDecimal("15000.00")
        );
        mockMvc.perform(post("/api/v2/nicolet/servicios")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicado)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        mockMvc.perform(delete("/api/v2/nicolet/servicios/" + servicioId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // Recrear servicio desactivado reactiva el registro existente
        ServicioCreateDTO reactivar = new ServicioCreateDTO(
                "lifting de pestañas qa",
                "Servicio reactivado",
                60,
                new BigDecimal("29990.00")
        );
        mockMvc.perform(post("/api/v2/nicolet/servicios")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reactivar)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(servicioId))
                .andExpect(jsonPath("$.activo").value(true))
                .andExpect(jsonPath("$.duracionMinutos").value(60));

        ServicioCreateDTO precioNegativo = new ServicioCreateDTO("Serv Malo", "Desc", 30, new BigDecimal("-5000"));
        mockMvc.perform(post("/api/v2/nicolet/servicios")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(precioNegativo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.precio").exists());

        ServicioCreateDTO duracionCorta = new ServicioCreateDTO("Serv Corto", "Desc", 5, new BigDecimal("10000"));
        mockMvc.perform(post("/api/v2/nicolet/servicios")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duracionCorta)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.duracionMinutos").exists());

        ServicioCreateDTO nombreVacio = new ServicioCreateDTO("   ", "Desc", 30, new BigDecimal("10000"));
        mockMvc.perform(post("/api/v2/nicolet/servicios")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(nombreVacio)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.nombre").exists());

        ServicioCreateDTO servicioLargo = new ServicioCreateDTO("Jornada Completa Novia", "12 horas", 720, new BigDecimal("150000"));
        MvcResult largoRes = mockMvc.perform(post("/api/v2/nicolet/servicios")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(servicioLargo)))
                .andExpect(status().isCreated())
                .andReturn();
        Long servLargoId = objectMapper.readTree(largoRes.getResponse().getContentAsString()).get("id").asLong();
        Long trabajadorId = trabajadorRepository.findByActivoTrue().get(0).getId();
        LocalDate proximoLunes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));

        mockMvc.perform(get("/api/v2/nicolet/disponibilidad")
                        .param("trabajadorId", trabajadorId.toString())
                        .param("fecha", proximoLunes.toString())
                        .param("servicioId", servLargoId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("Validar RUTs de clientes y reglas de horario en agendamiento de citas")
    void testReglasNegocioClientesTrabajadoresYCitas() throws Exception {
        UsuarioCreateDTO u1 = new UsuarioCreateDTO("CliUno", "SinRut", "cli1.sinrut@nicolet.cl", "Clave123", null);
        UsuarioCreateDTO u2 = new UsuarioCreateDTO("CliDos", "SinRut", "cli2.sinrut@nicolet.cl", "Clave123", null);

        Long u1Id = objectMapper.readTree(mockMvc.perform(post("/api/v2/nicolet/usuarios")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(u1))).andReturn().getResponse().getContentAsString()).get("id").asLong();

        Long u2Id = objectMapper.readTree(mockMvc.perform(post("/api/v2/nicolet/usuarios")
                .header("Authorization", "Bearer " + adminToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(u2))).andReturn().getResponse().getContentAsString()).get("id").asLong();

        ClienteCreateDTO c1Dto = new ClienteCreateDTO(u1Id, "+56911111111", "", LocalDate.of(1995, 1, 10), "Sin RUT 1");
        ClienteCreateDTO c2Dto = new ClienteCreateDTO(u2Id, "+56922222222", "   ", LocalDate.of(1996, 2, 20), "Sin RUT 2");

        Long c1Id = objectMapper.readTree(mockMvc.perform(post("/api/v2/nicolet/clientes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(c1Dto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        Long c2Id = objectMapper.readTree(mockMvc.perform(post("/api/v2/nicolet/clientes")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(c2Dto)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asLong();

        ClienteCreateDTO c2RutDuplicado = new ClienteCreateDTO(u2Id, "+56922222222", "  19876543-2  ", null, "Intento RUT duplicado");
        mockMvc.perform(put("/api/v2/nicolet/clientes/" + c2Id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(c2RutDuplicado)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        Long trabajadorId = trabajadorRepository.findByActivoTrue().get(0).getId();
        Long servicioId = servicioRepository.findByNombreIgnoreCase("Manicura Rusa con Esmaltado Permanente").orElseThrow().getId();
        LocalDate proximoMiercoles = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY));

        CitaCreateDTO citaValida = new CitaCreateDTO(c1Id, trabajadorId, servicioId, proximoMiercoles.atTime(9, 0), "Cita válida 09:00");
        MvcResult citaRes = mockMvc.perform(post("/api/v2/nicolet/citas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(citaValida)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andReturn();
        Long citaId = objectMapper.readTree(citaRes.getResponse().getContentAsString()).get("id").asLong();

        CitaCreateDTO citaSolapada = new CitaCreateDTO(c2Id, trabajadorId, servicioId, proximoMiercoles.atTime(9, 30), "Cita solapada");
        mockMvc.perform(post("/api/v2/nicolet/citas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(citaSolapada)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El profesional ya tiene una cita agendada en el horario solicitado"));

        CitaCreateDTO citaColacion = new CitaCreateDTO(c2Id, trabajadorId, servicioId, proximoMiercoles.atTime(12, 30), "Cruza colación");
        mockMvc.perform(post("/api/v2/nicolet/citas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(citaColacion)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        CitaCreateDTO citaFueraHorario = new CitaCreateDTO(c2Id, trabajadorId, servicioId, proximoMiercoles.atTime(17, 30), "Sale después de 18:00");
        mockMvc.perform(post("/api/v2/nicolet/citas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(citaFueraHorario)))
                .andExpect(status().isBadRequest());

        LocalDate proximoDomingo = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
        CitaCreateDTO citaDomingo = new CitaCreateDTO(c2Id, trabajadorId, servicioId, proximoDomingo.atTime(10, 0), "Cita domingo");
        mockMvc.perform(post("/api/v2/nicolet/citas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(citaDomingo)))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/v2/nicolet/citas/" + citaId + "/estado")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CambioEstadoCitaDTO(EstadoCita.COMPLETADA, "Atención impecable"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADA"));

        mockMvc.perform(patch("/api/v2/nicolet/citas/" + citaId + "/estado")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CambioEstadoCitaDTO(EstadoCita.CANCELADA, "Intento cancelar completada"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No se puede modificar el estado de una cita ya completada"));
    }

    @Test
    @DisplayName("Rechazar accesos sin token o con tokens JWT inválidos")
    void testPentestJwtYEndpointsProtegidos() throws Exception {
        mockMvc.perform(get("/api/v2/nicolet/usuarios")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v2/nicolet/usuarios").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v2/nicolet/usuarios/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v2/nicolet/citas")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v2/nicolet/citas").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v2/nicolet/servicios").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v2/nicolet/servicios/1")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v2/nicolet/clientes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v2/nicolet/tipos-usuario")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v2/nicolet/auth/me")).andExpect(status().isUnauthorized());

        SecretKey claveAtacante = Keys.hmacShaKeyFor("ClaveSecretaFalsaDelAtacanteDeAlMenos32BytesParaHmacSha256!".getBytes(StandardCharsets.UTF_8));
        String tokenFalsificado = Jwts.builder()
                .subject("admin.test@nicolet.cl")
                .claim("rol", "ROLE_ADMINISTRADOR")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(claveAtacante)
                .compact();

        mockMvc.perform(get("/api/v2/nicolet/usuarios")
                        .header("Authorization", "Bearer " + tokenFalsificado))
                .andExpect(status().isUnauthorized());

        String tokenAlgNone = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJzdWIiOiJhZG1pbi50ZXN0QG5pY29sZXQuY2wiLCJyb2wiOiJST0xFX0FETUlOSVNUUkFET1IifQ.";
        mockMvc.perform(get("/api/v2/nicolet/usuarios")
                        .header("Authorization", "Bearer " + tokenAlgNone))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v2/nicolet/usuarios")
                        .header("Authorization", "Bearer    "))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Bloquear IP tras intentos fallidos y mantener acceso a cuenta admin de prueba")
    void testFuerzaBrutaXForwardedForYExencionAdminTest() throws Exception {
        String ipAtacante = "198.51.100.77";
        LoginRequestDTO ataque = new LoginRequestDTO("martina.contreras@gmail.com", "ClaveErronea123");

        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/api/v2/nicolet/auth/login")
                            .header("X-Forwarded-For", ipAtacante)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(ataque)))
                    .andExpect(status().isUnauthorized());
        }

        LoginRequestDTO otroUsuarioDesdeMismaIp = new LoginRequestDTO("camila.silva@estudio.cl", "pro2026");
        mockMvc.perform(post("/api/v2/nicolet/auth/login")
                        .header("X-Forwarded-For", ipAtacante)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otroUsuarioDesdeMismaIp)))
                .andExpect(status().isTooManyRequests());

        LoginRequestDTO adminLogin = new LoginRequestDTO("admin.test@nicolet.cl", "AdminTest123!");
        mockMvc.perform(post("/api/v2/nicolet/auth/login")
                        .header("X-Forwarded-For", ipAtacante)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
    }

    @Test
    @DisplayName("Validar límite de 72 bytes en contraseñas, entradas inválidas y JSON malformado")
    void testPayloadsMaliciososBCrypt72BytesSqlInjectionYXss() throws Exception {
        RegistroRequestDTO regPassLarga = new RegistroRequestDTO(
                "Hacker", "Test", "hacker73@nicolet.cl", "X".repeat(80), "+56912345678"
        );
        mockMvc.perform(post("/api/v2/nicolet/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regPassLarga)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").exists());

        String passMultibyte = "ñ".repeat(45);
        LoginRequestDTO loginMultibyte = new LoginRequestDTO("maximo.rojas@estudio.cl", passMultibyte);
        mockMvc.perform(post("/api/v2/nicolet/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginMultibyte)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").exists());

        RegistroRequestDTO regMultibyte = new RegistroRequestDTO(
                "Hacker", "UTF8", "hacker.utf8@nicolet.cl", passMultibyte, "+56912345678"
        );
        mockMvc.perform(post("/api/v2/nicolet/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regMultibyte)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").exists());

        UsuarioCreateDTO userPassLarga = new UsuarioCreateDTO(
                "Hacker", "Admin", "hacker.user@nicolet.cl", "P".repeat(75), null
        );
        mockMvc.perform(post("/api/v2/nicolet/usuarios")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPassLarga)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").exists());

        UsuarioCreateDTO sqliUser = new UsuarioCreateDTO(
                "Robert'); DROP TABLE usuario;--",
                "Tables",
                "admin' OR '1'='1",
                "Clave12345",
                null
        );
        mockMvc.perform(post("/api/v2/nicolet/usuarios")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sqliUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.correo").exists());

        mockMvc.perform(get("/api/v2/nicolet/citas/codigo/' OR '1'='1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());

        assertThat(usuarioRepository.count()).isGreaterThanOrEqualTo(9);

        mockMvc.perform(post("/api/v2/nicolet/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"correo\": \"admin@nicolet.cl\", \"password\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        mockMvc.perform(post("/api/v2/nicolet/citas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"clienteId\": \"no-es-un-numero\", \"fechaHoraInicio\": \"fecha-rota\" }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
