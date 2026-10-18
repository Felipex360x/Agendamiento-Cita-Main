package cl.nicolet.backend.service;

import cl.nicolet.backend.dto.UsuarioCreateDTO;
import cl.nicolet.backend.dto.UsuarioDTO;
import cl.nicolet.backend.exception.RecursoNoEncontradoException;
import cl.nicolet.backend.model.Usuario;
import cl.nicolet.backend.repository.UsuarioRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    @DisplayName("findAll - debe retornar lista de usuarios cuando existen registros")
    void debeRetornarListaDeUsuario() {
        List<Usuario> usuariosSimulados = List.of(
            new Usuario(1L, "Martina", "Contreras", "Mar.con@gmail.com", "123412"),
            new Usuario(2L, "Paulina", "Contreras", "Pauli.con@gmail.com", "123412")
        );
        when(usuarioRepository.findAll()).thenReturn(usuariosSimulados);

        List<UsuarioDTO> resultado = usuarioService.findAll();

        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertEquals("Martina", resultado.get(0).getNombre());

        verify(usuarioRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("findAll - debe retornar lista vacía cuando no hay usuarios")
    void debeRetornarListaVaciaSiNoHayUsuario() {
        when(usuarioRepository.findAll()).thenReturn(List.of());

        List<UsuarioDTO> resultado = usuarioService.findAll();

        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    @Test
    @DisplayName("findById - debe retornar el usuario cuando existe")
    void debeRetornarUsuarioPorId() {
        Usuario usuario = new Usuario(
            1L, "Martina", "Contreras", "Mar.con@gmail.com", "123412"
        );
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        UsuarioDTO resultado = usuarioService.findById(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("Martina", resultado.getNombre());
        assertEquals("Mar.con@gmail.com", resultado.getCorreo());
    }

    @Test
    @DisplayName("findById - debe lanzar excepción cuando el ID no existe")
    void debeLanzarExcepcionCuandoUsuarioNoExiste() {
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> usuarioService.findById(999L));
    }

    @Test
    @DisplayName("crear - debe guardar y retornar el usuario creado")
    void debeCrearUsuarioCorrectamente() {
        UsuarioCreateDTO dto = new UsuarioCreateDTO(
            "Martina", "Contreras", "Mar.con@gmail.com", "123412"
        );
        Usuario guardado = new Usuario(
            3L, "Martina", "Contreras", "Mar.con@gmail.com", "123412"
        );
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(guardado);

        UsuarioDTO resultado = usuarioService.crear(dto);

        assertNotNull(resultado);
        assertEquals(3L, resultado.getId());
        assertEquals("Martina", resultado.getNombre());
        assertEquals("Mar.con@gmail.com", resultado.getCorreo());
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    @DisplayName("crear - debe cifrar la contraseña al guardar")
    void debeCifrarPasswordAlCrear() {
        UsuarioCreateDTO dto = new UsuarioCreateDTO("Ana", "Perez", "ana@gmail.com", "plaintxt123");
        when(passwordEncoder.encode("plaintxt123")).thenReturn("$2a$10$encodedpasswordhash");
        Usuario guardado = new Usuario(5L, "Ana", "Perez", "ana@gmail.com", "$2a$10$encodedpasswordhash");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(guardado);

        UsuarioDTO resultado = usuarioService.crear(dto);

        assertNotNull(resultado);
        verify(passwordEncoder, times(1)).encode("plaintxt123");
    }

    @Test
    @DisplayName("eliminar - debe lanzar excepción si el usuario no existe")
    void debeLanzarExcepcionAlEliminarUsuarioInexistente() {
        when(usuarioRepository.existsById(999L)).thenReturn(false);

        assertThrows(RecursoNoEncontradoException.class, () -> usuarioService.eliminar(999L));
        verify(usuarioRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("eliminar - debe eliminar el usuario cuando existe")
    void debeEliminarUsuarioExistente() {
        when(usuarioRepository.existsById(1L)).thenReturn(true);

        usuarioService.eliminar(1L);

        verify(usuarioRepository, times(1)).deleteById(1L);
    }
}