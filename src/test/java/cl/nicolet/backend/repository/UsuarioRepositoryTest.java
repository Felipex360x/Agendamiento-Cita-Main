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

@DataJpaTest
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("save - debe persistir el usuario y asignar un Id generado automaticamente")
    void debePersistirUsuarioYAsignarIdGenerado() {
        // Preparar datos
        Usuario usuario = new Usuario(
            null, "Martina", "Contreras", "Mar.con@gmail.com", "123412"
        );
        // Ejecutar
        Usuario guardado = usuarioRepository.save(usuario);
        // Comprobar
        assertNotNull(guardado.getId());
        assertTrue(guardado.getId() > 0);
        assertEquals("Martina", guardado.getNombre());
    }

    @Test
    @DisplayName("findAll - debe retornar todos los Usuarios guardados en la BD")
    void debeRetornarTodosLosUsuarios() {
        // Guardamos la cantidad inicial por si la base de datos ya tiene usuarios
        int antes = usuarioRepository.findAll().size();
        // Usamos correos distintos porque el correo no se puede repetir
        usuarioRepository.save(new Usuario(
            null, "Martina", "Contreras", "martina.repo@gmail.com", "123412"
        ));
        usuarioRepository.save(new Usuario(
            null, "Javiera", "Contreras", "javiera.repo@gmail.com", "123412"
        ));
        // Ejecutar
        List<Usuario> usuarios = usuarioRepository.findAll();
        // Comprobar
        assertNotNull(usuarios);
        assertEquals(antes + 2, usuarios.size());
    }

    @Test
    @DisplayName("findById - debe retornar el usuario cuando el ID existe")
    void debeEncontrarUsuarioPorIdExistente() {
        // Preparar datos
        Usuario guardado = usuarioRepository.save(new Usuario(
            null, "Martina", "Contreras", "Mar.con@gmail.com", "123412"
        ));
        // Ejecutar
        Optional<Usuario> resultado = usuarioRepository.findById(guardado.getId());
        // Comprobar
        assertTrue(resultado.isPresent());
        assertEquals("Martina", resultado.get().getNombre());
    } 

    @Test
    @DisplayName("findById - debe retornar Optional vacío cuando el ID no existe")
    void debeRetornarOptionalVacioCuandoIdNoExiste() {
        // Ejecutar
        Optional<Usuario> resultado = usuarioRepository.findById(999L);

        // Comprobar
        assertFalse(resultado.isPresent());
    }

    @Test
    @DisplayName("deleteById - debe eliminar el Usuario de la base de datos")
    void debeEliminarUsuarioPorId() {
        // Preparar datos
        Usuario guardado = usuarioRepository.save(new Usuario(
            null, "Daniela", "Contreras", "Mar.con@gmail.com", "123412"
        ));
        Long id = guardado.getId();
        // Ejecutar
        usuarioRepository.deleteById(id);
        // Comprobar
        assertFalse(usuarioRepository.findById(id).isPresent());
    }
}