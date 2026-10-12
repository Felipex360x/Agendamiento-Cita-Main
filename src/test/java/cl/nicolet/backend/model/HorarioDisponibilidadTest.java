package cl.nicolet.backend.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class HorarioDisponibilidadTest {

    @Test
    @DisplayName("Constructor vacio y defaults - activo debe ser true por defecto")
    void constructorVacioTenerActivoTrue() {
        HorarioDisponibilidad h = new HorarioDisponibilidad();
        assertNotNull(h);
        assertTrue(h.getActivo());
    }

    @Test
    @DisplayName("Constructor completo - debe mapear correctamente todos los atributos")
    void constructorCompletoDebeMapearAtributos() {
        Trabajador trabajador = new Trabajador();
        trabajador.setId(1L);

        LocalTime inicio = LocalTime.of(9, 0);
        LocalTime fin = LocalTime.of(18, 0);
        LocalTime inicioDescanso = LocalTime.of(13, 0);
        LocalTime finDescanso = LocalTime.of(14, 0);

        HorarioDisponibilidad h = new HorarioDisponibilidad(
                10L, trabajador, 1, inicio, fin, inicioDescanso, finDescanso, true
        );

        assertEquals(10L, h.getId());
        assertEquals(trabajador, h.getTrabajador());
        assertEquals(1, h.getDiaSemana());
        assertEquals(inicio, h.getHoraInicio());
        assertEquals(fin, h.getHoraFin());
        assertEquals(inicioDescanso, h.getHoraInicioDescanso());
        assertEquals(finDescanso, h.getHoraFinDescanso());
        assertTrue(h.getActivo());
    }

    @Test
    @DisplayName("Setters y Getters - debe actualizar y retornar campos correctamente")
    void settersYGettersDebenFuncionar() {
        HorarioDisponibilidad h = new HorarioDisponibilidad();
        h.setId(5L);
        h.setDiaSemana(3); // Miércoles
        h.setHoraInicio(LocalTime.of(10, 0));
        h.setHoraFin(LocalTime.of(19, 0));
        h.setActivo(false);

        assertEquals(5L, h.getId());
        assertEquals(3, h.getDiaSemana());
        assertEquals(LocalTime.of(10, 0), h.getHoraInicio());
        assertEquals(LocalTime.of(19, 0), h.getHoraFin());
        assertFalse(h.getActivo());
    }
}
