package cl.nicolet.backend.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class CitaTest {

    @Test
    @DisplayName("Constructor vacio y defaults - estado por defecto debe ser PENDIENTE")
    void constructorVacioTenerEstadoPendiente() {
        Cita c = new Cita();
        assertNotNull(c);
        assertEquals(EstadoCita.PENDIENTE, c.getEstado());
    }

    @Test
    @DisplayName("Constructor completo - debe mapear todos los atributos de la cita")
    void constructorCompletoDebeMapearAtributos() {
        Cliente cliente = new Cliente();
        cliente.setId(1L);

        Trabajador trabajador = new Trabajador();
        trabajador.setId(2L);

        Servicio servicio = new Servicio();
        servicio.setId(3L);

        LocalDateTime inicio = LocalDateTime.of(2026, 10, 15, 10, 0);
        LocalDateTime fin = LocalDateTime.of(2026, 10, 15, 11, 0);

        Cita c = new Cita(
                100L, "RES-2026-0001", cliente, trabajador, servicio,
                inicio, fin, EstadoCita.CONFIRMADA, new BigDecimal("22000"),
                "Notas cliente", "Notas trabajador", null
        );

        assertEquals(100L, c.getId());
        assertEquals("RES-2026-0001", c.getCodigoReserva());
        assertEquals(cliente, c.getCliente());
        assertEquals(trabajador, c.getTrabajador());
        assertEquals(servicio, c.getServicio());
        assertEquals(inicio, c.getFechaHoraInicio());
        assertEquals(fin, c.getFechaHoraFin());
        assertEquals(EstadoCita.CONFIRMADA, c.getEstado());
        assertEquals(new BigDecimal("22000"), c.getPrecioFinal());
        assertEquals("Notas cliente", c.getNotasCliente());
    }

    @Test
    @DisplayName("Setters - debe permitir actualizar estado y motivo de cancelacion")
    void settersEstadoYMotivo() {
        Cita c = new Cita();
        c.setEstado(EstadoCita.CANCELADA);
        c.setMotivoCancelacion("Fuerza mayor");

        assertEquals(EstadoCita.CANCELADA, c.getEstado());
        assertEquals("Fuerza mayor", c.getMotivoCancelacion());
    }
}
