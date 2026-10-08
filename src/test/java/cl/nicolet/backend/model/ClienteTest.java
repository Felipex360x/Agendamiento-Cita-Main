package cl.nicolet.backend.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class ClienteTest {

    @Test
    @DisplayName("Constructor vacio - debe crear una instancia no nula")
    void constructorVacioDebeCrearInstanciaNoNula() {
        Cliente c = new Cliente();
        assertNotNull(c);
        assertTrue(c.getActivo());
    }

    @Test
    @DisplayName("Constructor completo - debe asignar todos los campos correctamente")
    void constructorCompletoDebeAsignarCampos() {
        Usuario u = new Usuario(1L, "Martina", "Contreras", "m@c.cl", "123");
        LocalDate nacimiento = LocalDate.of(1998, 5, 14);
        Cliente c = new Cliente(10L, u, "+56912345678", "19876543-2", nacimiento, "Notas", true);

        assertEquals(10L, c.getId());
        assertEquals(u, c.getUsuario());
        assertEquals("+56912345678", c.getTelefono());
        assertEquals("19876543-2", c.getRutDni());
        assertEquals(nacimiento, c.getFechaNacimiento());
        assertEquals("Notas", c.getNotasPreferencias());
        assertTrue(c.getActivo());
    }

    @Test
    @DisplayName("Setters - debe permitir modificar cada campo individualmente")
    void settersDebenModificarCampos() {
        Cliente c = new Cliente();
        c.setId(20L);
        c.setTelefono("+56987654321");
        c.setRutDni("12345678-9");
        c.setActivo(false);

        assertEquals(20L, c.getId());
        assertEquals("+56987654321", c.getTelefono());
        assertEquals("12345678-9", c.getRutDni());
        assertFalse(c.getActivo());
    }

    @Test
    @DisplayName("equals y hashCode - dos clientes con mismos datos deben ser iguales")
    void dosClientesConMismosDatosDebenSerIguales() {
        Usuario u = new Usuario(1L, "Martina", "Contreras", "m@c.cl", "123");
        Cliente c1 = new Cliente(1L, u, "+569123", "123", null, null, true);
        Cliente c2 = new Cliente(1L, u, "+569123", "123", null, null, true);

        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
    }
}
