package cl.nicolet.backend.model;

import java.util.EnumSet;
import java.util.Set;

/**
 * Estados posibles de una cita y cambios permitidos:
 * <pre>
 * PENDIENTE   -> CONFIRMADA | CANCELADA
 * CONFIRMADA  -> EN_ATENCION | COMPLETADA | CANCELADA | NO_ASISTIO
 * EN_ATENCION -> COMPLETADA | CANCELADA
 * COMPLETADA, CANCELADA, NO_ASISTIO: estados finales (no cambian más; se debe crear una cita nueva).
 * </pre>
 * Solo una cita CANCELADA deja libre el horario del trabajador.
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
