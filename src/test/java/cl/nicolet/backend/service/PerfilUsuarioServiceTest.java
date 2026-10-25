package cl.nicolet.backend.service;

import cl.nicolet.backend.model.Cliente;
import cl.nicolet.backend.model.HorarioDisponibilidad;
import cl.nicolet.backend.model.Servicio;
import cl.nicolet.backend.model.TipoUsuario;
import cl.nicolet.backend.model.Trabajador;
import cl.nicolet.backend.model.Usuario;
import cl.nicolet.backend.repository.ClienteRepository;
import cl.nicolet.backend.repository.HorarioDisponibilidadRepository;
import cl.nicolet.backend.repository.ServicioRepository;
import cl.nicolet.backend.repository.TrabajadorRepository;
import cl.nicolet.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerfilUsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private TrabajadorRepository trabajadorRepository;

    @Mock
    private ServicioRepository servicioRepository;

    @Mock
    private HorarioDisponibilidadRepository horarioDisponibilidadRepository;

    @InjectMocks
    private PerfilUsuarioService perfilUsuarioService;

    private Usuario usuarioCliente;
    private Usuario usuarioProfesional;
    private Usuario usuarioAdmin;

    @BeforeEach
    void setUp() {
        TipoUsuario tipoCliente = new TipoUsuario(1L, "CLIENTE", "Rol cliente");
        TipoUsuario tipoPro = new TipoUsuario(2L, "PROFESIONAL", "Rol profesional");
        TipoUsuario tipoAdmin = new TipoUsuario(3L, "ADMINISTRADOR", "Rol admin");

        usuarioCliente = new Usuario(10L, "Laura", "Gomez", "laura@mail.com", "pass123");
        usuarioCliente.setTipoUsuario(tipoCliente);

        usuarioProfesional = new Usuario(20L, "Camila", "Silva", "camila@mail.com", "pass123");
        usuarioProfesional.setTipoUsuario(tipoPro);

        usuarioAdmin = new Usuario(30L, "Nicolet", "Estudio", "admin@mail.com", "pass123");
        usuarioAdmin.setTipoUsuario(tipoAdmin);
    }

    @Test
    @DisplayName("sincronizarPerfil - Crea y activa perfil de Cliente y desactiva Trabajador si existía")
    void sincronizarPerfil_Cliente_CreaPerfilYDesactivaTrabajador() {
        when(clienteRepository.findByUsuarioId(10L)).thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Trabajador trabajadorPrevio = new Trabajador();
        trabajadorPrevio.setId(100L);
        trabajadorPrevio.setActivo(true);
        when(trabajadorRepository.findByUsuarioId(10L)).thenReturn(Optional.of(trabajadorPrevio));

        perfilUsuarioService.sincronizarPerfil(usuarioCliente);

        verify(clienteRepository).save(any(Cliente.class));
        assertFalse(trabajadorPrevio.getActivo());
        verify(trabajadorRepository).save(trabajadorPrevio);
    }

    @Test
    @DisplayName("sincronizarPerfil - Crea perfil de Trabajador con horarios base para rol PROFESIONAL")
    void sincronizarPerfil_Profesional_CreaTrabajadorConHorarios() {
        when(trabajadorRepository.findByUsuarioId(20L)).thenReturn(Optional.empty());
        when(servicioRepository.findByActivoTrue()).thenReturn(List.of(new Servicio(1L, "Corte", null, null, null, true)));
        when(trabajadorRepository.save(any(Trabajador.class))).thenAnswer(invocation -> {
            Trabajador t = invocation.getArgument(0);
            t.setId(200L);
            return t;
        });
        when(horarioDisponibilidadRepository.findByTrabajadorId(200L)).thenReturn(Collections.emptyList());

        perfilUsuarioService.sincronizarPerfil(usuarioProfesional);

        verify(trabajadorRepository).save(any(Trabajador.class));
        verify(horarioDisponibilidadRepository, times(5)).save(any(HorarioDisponibilidad.class));
    }

    @Test
    @DisplayName("crearPerfilCliente - Asigna teléfono correctamente al crear cliente")
    void crearPerfilCliente_AsignaTelefono() {
        when(clienteRepository.findByUsuarioId(10L)).thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cliente resultado = perfilUsuarioService.crearPerfilCliente(usuarioCliente, "+56912345678");

        assertNotNull(resultado);
        assertEquals("+56912345678", resultado.getTelefono());
        assertEquals(usuarioCliente, resultado.getUsuario());
        assertTrue(resultado.getActivo());
    }

    @Test
    @DisplayName("crearPerfilesFaltantes - Repara perfiles de usuarios existentes que no los tengan")
    void crearPerfilesFaltantes_ReparaUsuariosSinPerfil() {
        when(usuarioRepository.findAll()).thenReturn(List.of(usuarioCliente, usuarioProfesional));
        when(clienteRepository.findByUsuarioId(10L)).thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(trabajadorRepository.findByUsuarioId(20L)).thenReturn(Optional.empty());
        when(servicioRepository.findByActivoTrue()).thenReturn(Collections.emptyList());
        when(trabajadorRepository.save(any(Trabajador.class))).thenAnswer(invocation -> {
            Trabajador t = invocation.getArgument(0);
            t.setId(201L);
            return t;
        });
        when(horarioDisponibilidadRepository.findByTrabajadorId(201L)).thenReturn(Collections.emptyList());

        perfilUsuarioService.crearPerfilesFaltantes();

        verify(clienteRepository).save(any(Cliente.class));
        verify(trabajadorRepository).save(any(Trabajador.class));
    }
}
