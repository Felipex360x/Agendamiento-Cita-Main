package cl.nicolet.backend.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TrabajadorTest {

    @Test
    @DisplayName("Constructor vacio - debe crear instancia no nula")
    void constructorVacioDebeCrearInstanciaNoNula() {
        Trabajador t = new Trabajador();
        assertNotNull(t);
        assertTrue(t.getActivo());
    }

    @Test
    @DisplayName("Constructor completo - debe asignar todos los atributos correctamente")
    void constructorCompletoDebeAsignarAtributos() {
        Usuario u = new Usuario(2L, "Camila", "Silva", "c@s.cl", "123");
        Set<Servicio> servicios = new HashSet<>();
        Trabajador t = new Trabajador(1L, u, "18234567-8", "+56987654321", "Estilista", "Bio", new BigDecimal("30.00"), true, servicios);

        assertEquals(1L, t.getId());
        assertEquals(u, t.getUsuario());
        assertEquals("18234567-8", t.getRutDni());
        assertEquals("+56987654321", t.getTelefono());
        assertEquals("Estilista", t.getCargoEspecialidad());
        assertEquals("Bio", t.getBiografia());
        assertEquals(new BigDecimal("30.00"), t.getComisionPorcentaje());
        assertTrue(t.getActivo());
        assertEquals(servicios, t.getServicios());
    }

    @Test
    @DisplayName("Setters y servicios - debe permitir asignar servicios al trabajador")
    void settersYServicios() {
        Trabajador t = new Trabajador();
        Servicio s = new Servicio(1L, "Corte", "Desc", 45, new BigDecimal("15000"), true);
        t.setServicios(Set.of(s));

        assertNotNull(t.getServicios());
        assertEquals(1, t.getServicios().size());
        assertTrue(t.getServicios().contains(s));
    }
}
