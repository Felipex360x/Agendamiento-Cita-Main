package cl.nicolet.backend_usuario.ServiceTest;

import cl.nicolet.backend_usuario.DTO.UsuarioCreateDTO;
import cl.nicolet.backend_usuario.DTO.UsuarioDTO;
import cl.nicolet.backend_usuario.Exception.RecursoNoEncontradoException;
import cl.nicolet.backend_usuario.Model.Usuario;
import cl.nicolet.backend_usuario.Repository.UsuarioRepository;
import cl.nicolet.backend_usuario.Service.UsuarioService;

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

    @InjectMocks
    private UsuarioService usuarioService;

    // -------------------------------- FINDALL ------------------------------------------------

    @Test
    @DisplayName("findAll - debe retornar lista de Usuario cuando existen registros")
    void debeRetornarListaDeUsuario() {
        // Given
        List<Usuario> usuariosSimulados = List.of(
            new Usuario(1L, "Martina", "Contreras", "Mar.con@gmail.com", "123412"),
            new Usuario(2L, "Paulina", "Contreras", "Pauli.con@gmail.com", "123412")
        );
        when(usuarioRepository.findAll()).thenReturn(usuariosSimulados);

        // When
        List<UsuarioDTO> resultado = usuarioService.findAll();

        // Then 
        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertEquals("Martina", resultado.get(0).getNombre());

        verify(usuarioRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("findAll - debe retornar lista vacia cuando no hay Usuario")
    void debeRetornarListaVaciaSiNoHayUsuario() {
        // Given
        when(usuarioRepository.findAll()).thenReturn(List.of());

        // When
        List<UsuarioDTO> resultado = usuarioService.findAll();

        // Then
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
    }

    // -------------------------------- FINDBYID ------------------------------------------------

    @Test
    @DisplayName("findById - debe retornar el DTO correcto cuando el usuario existe")
    void debeRetornarUsuarioPorId() {
        // Given
        Usuario usuario = new Usuario(
            1L, "Martina", "Contreras", "Mar.con@gmail.com", "123412"
        );
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        // When
        UsuarioDTO resultado = usuarioService.findById(1L);

        // Then
        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("Martina", resultado.getNombre());
        assertEquals("Mar.con@gmail.com", resultado.getCorreo());
    }

    @Test
    @DisplayName("findById - debe lanzar RecursoNoEncontradoException cuando el ID no existe")
    void debeLanzarExcepcionCuandoUsuarioNoExiste() {
        // Given
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RecursoNoEncontradoException.class, () -> usuarioService.findById(999L));
    }

    // ── crear ──────────────────────────────────────────────────────────────────
    @Test
    @DisplayName("crear - debe persistir y retornar el usuario con ID generado")
    void debeCrearUsuarioCorrectamente() {
        // Given
        UsuarioCreateDTO dto = new UsuarioCreateDTO(
            "Martina", "Contreras", "Mar.con@gmail.com", "123412"
        );
        Usuario guardado = new Usuario(
            3L, "Martina", "Contreras", "Mar.con@gmail.com", "123412"
        );
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(guardado);

        // When
        UsuarioDTO resultado = usuarioService.crear(dto);

        // Then  
        assertNotNull(resultado);
        assertEquals(3L, resultado.getId());
        assertEquals("Martina", resultado.getNombre());
        assertEquals("Mar.con@gmail.com", resultado.getCorreo());
        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    // ── eliminar ───────────────────────────────────────────────────────────────
    @Test
    @DisplayName("eliminar - debe lanzar excepción al intentar eliminar un ID inexistente")
    void debeLanzarExcepcionAlEliminarUsuarioInexistente() {
        // Given
        when(usuarioRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThrows(RecursoNoEncontradoException.class, () -> usuarioService.eliminar(999L));
        verify(usuarioRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("eliminar - debe invocar deleteById cuando el usuario existe")
    void debeEliminarUsuarioExistente() {
        // Given
        when(usuarioRepository.existsById(1L)).thenReturn(true);

        // When
        usuarioService.eliminar(1L);

        // Then
        verify(usuarioRepository, times(1)).deleteById(1L);
    }
}