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

    // =========================================================================
    // 1. CAOS CRUD DE USUARIOS, CAMBIO DE ROLES Y ELIMINACIÓN EN CASCADA
    // =========================================================================

    @Test
    @DisplayName("QA Stress: Creación masiva de usuarios, cambios de rol (Cliente <-> Profesional -> Admin), edición sin password y eliminación con citas/perfiles")
    void testCicloCaoticoUsuariosRolesYEliminacionEnCascada() throws Exception {
        Long rolClienteId = tipoUsuarioRepository.findByNombreIgnoreCase("CLIENTE").orElseThrow().getId();
        Long rolProId = tipoUsuarioRepository.findByNombreIgnoreCase("PROFESIONAL").orElseThrow().getId();
        Long rolAdminId = tipoUsuarioRepository.findByNombreIgnoreCase("ADMINISTRADOR").orElseThrow().getId();

        List<Long> usuariosCreados = new ArrayList<>();

        // 1. Crear 10 usuarios caóticos (mezcla de Clientes, Profesionales y Administradores)
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

        // 2. Tomar un usuario CLIENTE (userId 0 -> i=1) y cambiar su rol a PROFESIONAL, luego a CLIENTE, luego de vuelta a PROFESIONAL
        Long userMutanteId = usuariosCreados.get(0);
        UsuarioCreateDTO updateToPro = new UsuarioCreateDTO(
                "MutantePro",
                "Cambiado",
                "stress.user.1@nicolet.cl",
                "", // Password vacía: debe mantener la anterior
                rolProId
        );
        mockMvc.perform(put("/api/v2/nicolet/usuarios/" + userMutanteId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateToPro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("MutantePro"))
                .andExpect(jsonPath("$.tipoUsuario.nombre").value("PROFESIONAL"));

        // Verificar que ahora tiene perfil de Trabajador activo y que su contraseña original sigue funcionando
        assertThat(trabajadorRepository.findByUsuarioId(userMutanteId)).isPresent();
        assertThat(trabajadorRepository.findByUsuarioId(userMutanteId).get().getActivo()).isTrue();

        mockMvc.perform(post("/api/v2/nicolet/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequestDTO("stress.user.1@nicolet.cl", "ClaveSegura#1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("PROFESIONAL"));

        // Cambiar de PROFESIONAL de vuelta a CLIENTE y cambiar contraseña
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

        // El perfil de trabajador debe haberse desactivado al pasar a CLIENTE
        assertThat(trabajadorRepository.findByUsuarioId(userMutanteId).get().getActivo()).isFalse();

        // Volver a pasar a PROFESIONAL para agendarle citas tanto como cliente como trabajador
        updateToPro.setPassword("NuevaClaveMutante#99");
        mockMvc.perform(put("/api/v2/nicolet/usuarios/" + userMutanteId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateToPro)))
                .andExpect(status().isOk());
        assertThat(trabajadorRepository.findByUsuarioId(userMutanteId).get().getActivo()).isTrue();

        // 3. Agendar citas vinculadas a los usuarios creados antes de eliminarlos
        Long clienteId = clienteRepository.findByUsuarioId(userMutanteId).orElseThrow().getId();
        Long trabajadorId = trabajadorRepository.findByUsuarioId(usuariosCreados.get(1)).orElseThrow().getId();
        Long servicioId = servicioRepository.findByActivoTrue().get(0).getId();

        LocalDate proximoMartes = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.TUESDAY));
        CitaCreateDTO citaDto = new CitaCreateDTO(
                clienteId,
                trabajadorId,
                servicioId,
                proximoMartes.atTime(10, 0),
                "Cita de prueba antes de demolición de usuarios"
        );

        mockMvc.perform(post("/api/v2/nicolet/citas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(citaDto)))
                .andExpect(status().isCreated());

        // 4. Eliminar TODOS los usuarios creados (incluyendo los que tienen Cliente + Trabajador + Horarios + Servicios + Citas)
        for (Long uId : usuariosCreados) {
            mockMvc.perform(delete("/api/v2/nicolet/usuarios/" + uId)
                            .header("Authorization", "Bearer " + adminToken))
                    .andExpect(status().isNoContent());
            assertThat(usuarioRepository.findById(uId)).isEmpty();
        }
    }

    @Test
    @DisplayName("QA Casos Borde Usuarios: Duplicados case-insensitive, tipoUsuarioId inexistente y validación manual")
    void testCasosBordeUsuarios() throws Exception {
        // 1. Intento de crear usuario con correo ya existente en mayúsculas y con espacios
        UsuarioCreateDTO dupDto = new UsuarioCreateDTO(
                "Clon", "Rojas", "  MAXIMO.ROJAS@ESTUDIO.CL  ", "Clave12345", null
        );
        mockMvc.perform(post("/api/v2/nicolet/usuarios")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dupDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        // 2. Endpoint /manual con correo duplicado
        mockMvc.perform(post("/api/v2/nicolet/usuarios/manual")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dupDto)))
                .andExpect(status().isBadRequest());

        // 3. Intento de crear usuario con tipoUsuarioId que NO existe (999999L) -> 404 Not Found
        UsuarioCreateDTO rolInexistenteDto = new UsuarioCreateDTO(
                "Fantasma", "Rol", "fantasma.rol@nicolet.cl", "Clave12345", 999999L
        );
        mockMvc.perform(post("/api/v2/nicolet/usuarios")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rolInexistenteDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());

        // 4. Intento de actualizar un usuario existente con el correo de otro usuario
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

        // 5. Intento de actualizar usuario asignándole un tipoUsuarioId inexistente -> 404 Not Found
        UsuarioCreateDTO colisionRol = new UsuarioCreateDTO(
                "Martina", "Contreras", "martina.contreras@gmail.com", "", 888888L
        );
        mockMvc.perform(put("/api/v2/nicolet/usuarios/" + idMartina)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(colisionRol)))
                .andExpect(status().isNotFound());
    }

    // =========================================================================
    // 2. SERVICIOS: CICLO DE VIDA, REACTIVACIÓN Y VALIDACIONES EXTREMAS
    // =========================================================================

    @Test
    @DisplayName("QA Servicios: Crear, editar, desactivar, recrear con mismo nombre, precios negativos y bucle infinito DoS")
    void testCicloServiciosYValidaciones() throws Exception {
        // 1. Crear servicio nuevo
        ServicioCreateDTO nuevo = new ServicioCreateDTO(
                "  Lifting de Pestañas QA  ",
                "Servicio de prueba QA",
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

        // 2. Intentar crear duplicado mientras está activo (con mayúsculas y espacios) -> 400 Bad Request
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

        // 3. Desactivar servicio (baja lógica)
        mockMvc.perform(delete("/api/v2/nicolet/servicios/" + servicioId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        // 4. Recrear servicio desactivado con el mismo nombre y nuevo precio -> Debe reactivar el mismo ID
        ServicioCreateDTO reactivar = new ServicioCreateDTO(
                "lifting de pestañas qa",
                "Servicio reactivado y actualizado",
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

        // 5. Validaciones de precios negativos, cero, duración < 10 min y nombre vacío
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

        // 6. Prueba de estrés anti-bucle infinito en DisponibilidadService con servicio de 720 minutos (12 horas)
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

        // No debe colgarse en bucle infinito; debe retornar lista vacía (200 OK)
        mockMvc.perform(get("/api/v2/nicolet/disponibilidad")
                        .param("trabajadorId", trabajadorId.toString())
                        .param("fecha", proximoLunes.toString())
                        .param("servicioId", servLargoId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // =========================================================================
    // 3. CLIENTES, TRABAJADORES Y REGLAS ESTRICTAS DE AGENDAMIENTO DE CITAS
    // =========================================================================

    @Test
    @DisplayName("QA Clientes, Trabajadores y Citas: RUTs vacíos/duplicados, colación, fuera de jornada, solapamientos y citas completadas")
    void testReglasNegocioClientesTrabajadoresYCitas() throws Exception {
        // 1. Crear dos usuarios sin rol inicial para probar creación manual de perfiles de Cliente con RUT vacío ""
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

        // Ambos envían rutDni = "" (cadena vacía): no debe fallar por UNIQUE constraint
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

        // Intentar actualizar c2 con el RUT existente de Martina ("19876543-2") con espacios alrededor -> 400 Bad Request
        ClienteCreateDTO c2RutDuplicado = new ClienteCreateDTO(u2Id, "+56922222222", "  19876543-2  ", null, "Intento RUT duplicado");
        mockMvc.perform(put("/api/v2/nicolet/clientes/" + c2Id)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(c2RutDuplicado)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        // 2. Pruebas de agendamiento de citas (Camila atiende Lunes a Viernes 09:00-18:00, descanso 13:00-14:00)
        Long trabajadorId = trabajadorRepository.findByActivoTrue().get(0).getId();
        Long servicioId = servicioRepository.findByNombreIgnoreCase("Manicura Rusa con Esmaltado Permanente").orElseThrow().getId(); // 60 min
        LocalDate proximoMiercoles = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY));

        // 2a. Agendar cita válida el próximo miércoles de 09:00 a 10:00
        CitaCreateDTO citaValida = new CitaCreateDTO(c1Id, trabajadorId, servicioId, proximoMiercoles.atTime(9, 0), "Cita válida 09:00");
        MvcResult citaRes = mockMvc.perform(post("/api/v2/nicolet/citas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(citaValida)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andReturn();
        Long citaId = objectMapper.readTree(citaRes.getResponse().getContentAsString()).get("id").asLong();

        // 2b. Intentar agendar cita solapada (09:30 a 10:30) -> 400 Bad Request
        CitaCreateDTO citaSolapada = new CitaCreateDTO(c2Id, trabajadorId, servicioId, proximoMiercoles.atTime(9, 30), "Cita solapada");
        mockMvc.perform(post("/api/v2/nicolet/citas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(citaSolapada)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El profesional ya tiene una cita agendada en el horario solicitado"));

        // 2c. Intentar agendar en horario de colación/descanso (13:00 a 14:00 o 12:30 a 13:30) -> 400 Bad Request
        CitaCreateDTO citaColacion = new CitaCreateDTO(c2Id, trabajadorId, servicioId, proximoMiercoles.atTime(12, 30), "Cruza colación");
        mockMvc.perform(post("/api/v2/nicolet/citas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(citaColacion)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());

        // 2d. Intentar agendar fuera de horario laboral (08:00 o 17:30 terminando 18:30) -> 400 Bad Request
        CitaCreateDTO citaFueraHorario = new CitaCreateDTO(c2Id, trabajadorId, servicioId, proximoMiercoles.atTime(17, 30), "Sale después de 18:00");
        mockMvc.perform(post("/api/v2/nicolet/citas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(citaFueraHorario)))
                .andExpect(status().isBadRequest());

        // 2e. Intentar agendar en día Domingo (sin turno configurado) -> 400 Bad Request
        LocalDate proximoDomingo = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.SUNDAY));
        CitaCreateDTO citaDomingo = new CitaCreateDTO(c2Id, trabajadorId, servicioId, proximoDomingo.atTime(10, 0), "Cita domingo");
        mockMvc.perform(post("/api/v2/nicolet/citas")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(citaDomingo)))
                .andExpect(status().isBadRequest());

        // 2f. Completar la cita válida e intentar cancelarla después -> 400 Bad Request
        mockMvc.perform(patch("/api/v2/nicolet/citas/" + citaId + "/estado")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CambioEstadoCitaDTO(EstadoCita.COMPLETADA, "Atención impecable"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("COMPLETADA"));

        mockMvc.perform(patch("/api/v2/nicolet/citas/" + citaId + "/estado")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CambioEstadoCitaDTO(EstadoCita.CANCELADA, "Intento ilegal de cancelar completada"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("No se puede modificar el estado de una cita ya completada"));
    }

    // =========================================================================
    // 4. PENTESTING DE SEGURIDAD: JWT, RATE LIMITING, SQLi, XSS, BCRYPT >72 BYTES
    // =========================================================================

    @Test
    @DisplayName("Seguridad Pentest: Rechazo de accesos sin JWT y con JWT manipulados/expirados en todos los módulos protegidos")
    void testPentestJwtYEndpointsProtegidos() throws Exception {
        // 1. Peticiones sin token a rutas críticas deben retornar 401 Unauthorized
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

        // 2. Token firmado con otra clave secreta (ataque de falsificación de firma)
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

        // 3. Token con algoritmo 'none' (sin firma)
        String tokenAlgNone = "eyJhbGciOiJub25lIiwidHlwIjoiSldUIn0.eyJzdWIiOiJhZG1pbi50ZXN0QG5pY29sZXQuY2wiLCJyb2wiOiJST0xFX0FETUlOSVNUUkFET1IifQ.";
        mockMvc.perform(get("/api/v2/nicolet/usuarios")
                        .header("Authorization", "Bearer " + tokenAlgNone))
                .andExpect(status().isUnauthorized());

        // 4. Token vacío o corrupto
        mockMvc.perform(get("/api/v2/nicolet/usuarios")
                        .header("Authorization", "Bearer    "))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Seguridad Pentest: Fuerza bruta con X-Forwarded-For bloquea IP externa al 6° intento pero exime a admin.test@nicolet.cl")
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

        // Al 6° intento desde la misma IP externa (incluso cambiando de correo a otro usuario normal), la IP está bloqueada (429)
        LoginRequestDTO otroUsuarioDesdeMismaIp = new LoginRequestDTO("camila.silva@estudio.cl", "pro2026");
        mockMvc.perform(post("/api/v2/nicolet/auth/login")
                        .header("X-Forwarded-For", ipAtacante)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(otroUsuarioDesdeMismaIp)))
                .andExpect(status().isTooManyRequests());

        // Sin embargo, admin.test@nicolet.cl NUNCA es bloqueado aunque venga desde la IP bloqueada
        LoginRequestDTO adminLogin = new LoginRequestDTO("admin.test@nicolet.cl", "AdminTest123!");
        mockMvc.perform(post("/api/v2/nicolet/auth/login")
                        .header("X-Forwarded-For", ipAtacante)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("ADMINISTRADOR"));
    }

    @Test
    @DisplayName("Seguridad Pentest: Límite BCrypt >72 caracteres y >72 bytes UTF-8 multibyte, SQLi, XSS y JSON corrupto")
    void testPayloadsMaliciososBCrypt72BytesSqlInjectionYXss() throws Exception {
        // 1. Contraseña > 72 caracteres ASCII en /auth/register -> 400 Bad Request
        RegistroRequestDTO regPassLarga = new RegistroRequestDTO(
                "Hacker", "Test", "hacker73@nicolet.cl", "X".repeat(80), "+56912345678"
        );
        mockMvc.perform(post("/api/v2/nicolet/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(regPassLarga)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").exists());

        // 2. Contraseña con caracteres multibyte UTF-8 (<= 72 chars pero > 72 bytes, ej. 45 'ñ' = 45 chars pero 90 bytes UTF-8)
        // Antes causaba HTTP 500 en /auth/login por BCrypt; ahora debe retornar 400 Bad Request limpio
        String passMultibyte = "ñ".repeat(45); // 45 chars (<=72), 90 bytes (>72)
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

        // 3. Contraseña > 72 caracteres en POST /api/v2/nicolet/usuarios -> 400 Bad Request
        UsuarioCreateDTO userPassLarga = new UsuarioCreateDTO(
                "Hacker", "Admin", "hacker.user@nicolet.cl", "P".repeat(75), null
        );
        mockMvc.perform(post("/api/v2/nicolet/usuarios")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPassLarga)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").exists());

        // 4. Inyección SQL en correo de creación de usuario y registro -> 400 Bad Request
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

        // 5. Inyección SQL en búsqueda de cita por código -> 404 Not Found (parametrizado por JPA, tabla intacta)
        mockMvc.perform(get("/api/v2/nicolet/citas/codigo/' OR '1'='1")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound());

        // Verificar que las tablas siguen intactas tras intentos de SQLi
        assertThat(usuarioRepository.count()).isGreaterThanOrEqualTo(9);

        // 6. JSON malformado y tipos de datos rotos -> 400 Bad Request (nunca 500)
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
