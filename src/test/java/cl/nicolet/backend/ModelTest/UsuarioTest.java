package cl.nicolet.backend.ModelTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import cl.nicolet.backend.Model.Usuario;

class UsuarioTest {

    @Test
    @DisplayName("Constructor vacio - debe crear una instancia no nula")
    void constructorVacioDebeCrearInstanciaNoNula() {
        Usuario usuario = new Usuario();
        assertNotNull(usuario);
    }

    @Test
    @DisplayName("Constructor completo - debe asignar todos los campos correctamente")
    void constructorCompletoDebeAsignarTodosLosCampos() {
        Usuario usuario = new Usuario(
            1L, "Martina", "Contreras", "Mar.con@gmail.com", "123412"
        );

        assertEquals(1L, usuario.getId());
        assertEquals("Martina", usuario.getNombre());
        assertEquals("Contreras", usuario.getApellidoP());
        assertEquals("Mar.con@gmail.com", usuario.getCorreo());
        assertEquals("123412", usuario.getPassword());
    }

    @Test
    @DisplayName("Setters - debe permitir modificar cada campo individualmente")
    void settersDebenPermitirModificarCampos() {
        Usuario usuario = new Usuario();

        usuario.setId(2L);
        usuario.setNombre("Milla");
        usuario.setApellidoP("Perez");
        usuario.setCorreo("Milla@gmail.com");
        usuario.setPassword("abc123");

        assertEquals(2L, usuario.getId());
        assertEquals("Milla", usuario.getNombre());
        assertEquals("Perez", usuario.getApellidoP());
        assertEquals("Milla@gmail.com", usuario.getCorreo());
        assertEquals("abc123", usuario.getPassword());
    }

    @Test
    @DisplayName("equals y hashCode - dos Usuarios con los mismos datos deben ser iguales")
    void dosUsuariosConMismosDatosDebenSerIguales() {
        Usuario u1 = new Usuario(
            1L, "Martina", "Contreras", "Mar.con@gmail.com", "123412"
        );

        Usuario u2 = new Usuario(
            1L, "Martina", "Contreras", "Mar.con@gmail.com", "123412"
        );

        assertEquals(u1, u2);
        assertEquals(u1.hashCode(), u2.hashCode());
    }

    @Test
    @DisplayName("ToString - debe contener el nombre del usuario en la representacion")
    void toStringDebeContenerNombreDelUsuario() {
        Usuario usuario = new Usuario(
            3L, "Martina", "Contreras", "Mar.con@gmail.com", "123412"
        );
        String texto = usuario.toString();

        assertNotNull(texto);
        assertTrue(texto.contains("Martina"));
    }
}