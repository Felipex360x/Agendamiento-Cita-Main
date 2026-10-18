package cl.nicolet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalTime;
import java.util.Objects;

@Schema(description = "Bloque horario calculado con estado de disponibilidad")
public class BloqueHorarioDTO {

    @Schema(description = "Hora de inicio del bloque", example = "10:00:00")
    private LocalTime horaInicio;

    @Schema(description = "Hora estimada de término según duración del servicio", example = "10:45:00")
    private LocalTime horaFin;

    @Schema(description = "Indica si el bloque está libre para ser agendado", example = "true")
    private Boolean disponible;

    @Schema(description = "Motivo por el que no está disponible (null si está libre)", example = "OCUPADO")
    private String motivoNoDisponible;

    public BloqueHorarioDTO() {
    }

    public BloqueHorarioDTO(LocalTime horaInicio, LocalTime horaFin, Boolean disponible, String motivoNoDisponible) {
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.disponible = disponible;
        this.motivoNoDisponible = motivoNoDisponible;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public void setHoraInicio(LocalTime horaInicio) {
        this.horaInicio = horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public void setHoraFin(LocalTime horaFin) {
        this.horaFin = horaFin;
    }

    public Boolean getDisponible() {
        return disponible;
    }

    public void setDisponible(Boolean disponible) {
        this.disponible = disponible;
    }

    public String getMotivoNoDisponible() {
        return motivoNoDisponible;
    }

    public void setMotivoNoDisponible(String motivoNoDisponible) {
        this.motivoNoDisponible = motivoNoDisponible;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BloqueHorarioDTO that)) return false;
        return Objects.equals(horaInicio, that.horaInicio)
                && Objects.equals(horaFin, that.horaFin)
                && Objects.equals(disponible, that.disponible)
                && Objects.equals(motivoNoDisponible, that.motivoNoDisponible);
    }

    @Override
    public int hashCode() {
        return Objects.hash(horaInicio, horaFin, disponible, motivoNoDisponible);
    }
}
