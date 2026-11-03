package cl.nicolet.backend.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados de una cita y máquina de transiciones.
 * <pre>
 * PENDIENTE   -> CONFIRMADA | CANCELADA
 * CONFIRMADA  -> EN_ATENCION | COMPLETADA | CANCELADA | NO_ASISTIO
 * EN_ATENCION -> COMPLETADA | CANCELADA
 * COMPLETADA, CANCELADA, NO_ASISTIO: estados finales (no se reactivan; se crea una nueva reserva).
 * </pre>
 * Solo CANCELADA libera el horario del profesional; el resto de los estados lo ocupa.
 */
public enum EstadoCita {
    PENDIENTE,
    CONFIRMADA,
    EN_ATENCION,
    COMPLETADA,
    CANCELADA,
    NO_ASISTIO;

    public Set<EstadoCita> transicionesPermitidas() {
        return switch (this) {
            case PENDIENTE -> EnumSet.of(CONFIRMADA, CANCELADA);
            case CONFIRMADA -> EnumSet.of(EN_ATENCION, COMPLETADA, CANCELADA, NO_ASISTIO);
            case EN_ATENCION -> EnumSet.of(COMPLETADA, CANCELADA);
            case COMPLETADA, CANCELADA, NO_ASISTIO -> EnumSet.noneOf(EstadoCita.class);
        };
    }

    public boolean esFinal() {
        return transicionesPermitidas().isEmpty();
    }

    public boolean puedePasarA(EstadoCita nuevo) {
        return transicionesPermitidas().contains(nuevo);
    }

    public boolean ocupaHorario() {
        return this != CANCELADA;
    }
}
