package cl.nicolet.backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import cl.nicolet.backend.model.Usuario;
import cl.nicolet.backend.repository.UsuarioRepository;

@DataJpaTest
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("save - debe persistir el usuario y asignar un Id generado automaticamente")
    void debePersistirUsuarioYAsignarIdGenerado() {
        // Given
        Usuario usuario = new Usuario(
            null, "Martina", "Contreras", "Mar.con@gmail.com", "123412"
        );
        // When
        Usuario guardado = usuarioRepository.save(usuario);
        // Then
        assertNotNull(guardado.getId());
        assertTrue(guardado.getId() > 0);
        assertEquals("Martina", guardado.getNombre());
    }

    @Test
    @DisplayName("findAll - debe retornar todos los Usuarios guardados en la BD")
    void debeRetornarTodosLosUsuarios() {
        // Given
        usuarioRepository.save(new Usuario(
            null, "Martina", "Contreras", "Mar.con@gmail.com", "123412"
        ));
        usuarioRepository.save(new Usuario(
            null, "Javiera", "Contreras", "Mar.con@gmail.com", "123412"
        ));
        // When
        List<Usuario> usuarios = usuarioRepository.findAll();
        // Then
        assertNotNull(usuarios);
        assertEquals(2, usuarios.size());
    }

    @Test
    @DisplayName("findById - debe retornar el usuario cuando el ID existe")
    void debeEncontrarUsuarioPorIdExistente() {
        // Given
        Usuario guardado = usuarioRepository.save(new Usuario(
            null, "Martina", "Contreras", "Mar.con@gmail.com", "123412"
        ));
        // When
        Optional<Usuario> resultado = usuarioRepository.findById(guardado.getId());
        // Then
        assertTrue(resultado.isPresent());
        assertEquals("Martina", resultado.get().getNombre());
    } 

    @Test
    @DisplayName("findById - debe retornar Optional vacío cuando el ID no existe")
    void debeRetornarOptionalVacioCuandoIdNoExiste() {
        // When
        Optional<Usuario> resultado = usuarioRepository.findById(999L);

        // Then
        assertFalse(resultado.isPresent());
    }

    @Test
    @DisplayName("deleteById - debe eliminar el Usuario de la base de datos")
    void debeEliminarUsuarioPorId() {
        // Given
        Usuario guardado = usuarioRepository.save(new Usuario(
            null, "Daniela", "Contreras", "Mar.con@gmail.com", "123412"
        ));
        Long id = guardado.getId();
        // When
        usuarioRepository.deleteById(id);
        // Then
        assertFalse(usuarioRepository.findById(id).isPresent());
    }
}