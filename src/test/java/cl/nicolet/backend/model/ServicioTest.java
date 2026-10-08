package cl.nicolet.backend.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ServicioTest {

    @Test
    @DisplayName("Constructor vacio y defaults - activo debe ser true por defecto")
    void constructorVacioDebeTenerActivoTrue() {
        Servicio s = new Servicio();
        assertNotNull(s);
        assertTrue(s.getActivo());
    }

    @Test
    @DisplayName("Constructor completo - debe mapear campos")
    void constructorCompletoDebeMapearCampos() {
        Servicio s = new Servicio(5L, "Manicura Rusa", "Limpieza", 60, new BigDecimal("22000"), true);

        assertEquals(5L, s.getId());
        assertEquals("Manicura Rusa", s.getNombre());
        assertEquals("Limpieza", s.getDescripcion());
        assertEquals(60, s.getDuracionMinutos());
        assertEquals(new BigDecimal("22000"), s.getPrecio());
        assertTrue(s.getActivo());
    }

    @Test
    @DisplayName("equals y hashCode - dos servicios con mismos datos deben ser iguales")
    void dosServiciosConMismosDatosDebenSerIguales() {
        Servicio s1 = new Servicio(1L, "Corte", "Desc", 30, new BigDecimal("10000"), true);
        Servicio s2 = new Servicio(1L, "Corte", "Desc", 30, new BigDecimal("10000"), true);

        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());
    }
}
