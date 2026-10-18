package cl.nicolet.backend.dto;

import cl.nicolet.backend.model.EstadoCita;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.util.Objects;

public class CambioEstadoCitaDTO {

    @Schema(description = "Nuevo estado de la cita", example = "CONFIRMADA")
    @NotNull(message = "El nuevo estado es obligatorio")
    private EstadoCita nuevoEstado;

    @Schema(description = "Motivo de cancelación o notas del cambio", example = "Cancelado por el cliente con 24h de anticipación")
    private String motivoONotas;

    public CambioEstadoCitaDTO() {
    }

    public CambioEstadoCitaDTO(EstadoCita nuevoEstado, String motivoONotas) {
        this.nuevoEstado = nuevoEstado;
        this.motivoONotas = motivoONotas;
    }

    public EstadoCita getNuevoEstado() {
        return nuevoEstado;
    }

    public void setNuevoEstado(EstadoCita nuevoEstado) {
        this.nuevoEstado = nuevoEstado;
    }

    public String getMotivoONotas() {
        return motivoONotas;
    }

    public void setMotivoONotas(String motivoONotas) {
        this.motivoONotas = motivoONotas;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CambioEstadoCitaDTO that)) return false;
        return nuevoEstado == that.nuevoEstado && Objects.equals(motivoONotas, that.motivoONotas);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nuevoEstado, motivoONotas);
    }
}
