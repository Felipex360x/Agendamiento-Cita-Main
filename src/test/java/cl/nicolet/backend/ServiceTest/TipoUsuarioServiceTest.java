package cl.nicolet.backend.ServiceTest;

import cl.nicolet.backend.DTO.TipoUsuarioCreateDTO;
import cl.nicolet.backend.DTO.TipoUsuarioDTO;
import cl.nicolet.backend.Exception.RecursoNoEncontradoException;
import cl.nicolet.backend.Model.TipoUsuario;
import cl.nicolet.backend.Repository.TipoUsuarioRepository;
import cl.nicolet.backend.Service.TipoUsuarioService;
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
class TipoUsuarioServiceTest {

    @Mock
    private TipoUsuarioRepository tipoUsuarioRepository;

    @InjectMocks
    private TipoUsuarioService tipoUsuarioService;

    @Test
    @DisplayName("findAll - debe retornar lista de tipos de usuario")
    void debeRetornarListaDeTiposDeUsuario() {
        List<TipoUsuario> lista = List.of(
            new TipoUsuario(1L, "ADMINISTRADOR", "Acceso total"),
            new TipoUsuario(2L, "CLIENTE", "Cliente")
        );
        when(tipoUsuarioRepository.findAll()).thenReturn(lista);

        List<TipoUsuarioDTO> resultado = tipoUsuarioService.findAll();

        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertEquals("ADMINISTRADOR", resultado.get(0).getNombre());
    }

    @Test
    @DisplayName("findById - debe retornar DTO cuando existe")
    void debeRetornarTipoUsuarioPorId() {
        TipoUsuario tu = new TipoUsuario(1L, "ADMINISTRADOR", "Acceso total");
        when(tipoUsuarioRepository.findById(1L)).thenReturn(Optional.of(tu));

        TipoUsuarioDTO resultado = tipoUsuarioService.findById(1L);

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("ADMINISTRADOR", resultado.getNombre());
    }

    @Test
    @DisplayName("findById - debe lanzar RecursoNoEncontradoException cuando no existe")
    void debeLanzarExcepcionCuandoNoExiste() {
        when(tipoUsuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> tipoUsuarioService.findById(99L));
    }

    @Test
    @DisplayName("crear - debe guardar y retornar nuevo tipo de usuario")
    void debeCrearTipoUsuarioCorrectamente() {
        TipoUsuarioCreateDTO dto = new TipoUsuarioCreateDTO("RECEPCIONISTA", "Atención al cliente");
        TipoUsuario guardado = new TipoUsuario(4L, "RECEPCIONISTA", "Atención al cliente");
        when(tipoUsuarioRepository.save(any(TipoUsuario.class))).thenReturn(guardado);

        TipoUsuarioDTO resultado = tipoUsuarioService.crear(dto);

        assertNotNull(resultado);
        assertEquals(4L, resultado.getId());
        assertEquals("RECEPCIONISTA", resultado.getNombre());
    }

    @Test
    @DisplayName("crear - debe lanzar IllegalArgumentException cuando el rol ya existe")
    void debeLanzarExcepcionCuandoRolYaExiste() {
        TipoUsuarioCreateDTO dto = new TipoUsuarioCreateDTO("ADMINISTRADOR", "Acceso");
        when(tipoUsuarioRepository.existsByNombreIgnoreCase("ADMINISTRADOR")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> tipoUsuarioService.crear(dto));
    }

    @Test
    @DisplayName("eliminar - debe eliminar tipo de usuario existente")
    void debeEliminarTipoUsuario() {
        when(tipoUsuarioRepository.existsById(1L)).thenReturn(true);

        tipoUsuarioService.eliminar(1L);

        verify(tipoUsuarioRepository, times(1)).deleteById(1L);
    }
}
