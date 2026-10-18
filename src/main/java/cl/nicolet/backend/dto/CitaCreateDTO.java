package cl.nicolet.backend.dto;

import cl.nicolet.backend.model.EstadoCita;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.Objects;

public class CitaCreateDTO {

    @Schema(description = "ID del cliente que solicita la cita", example = "1")
    @NotNull(message = "El clienteId es obligatorio")
    private Long clienteId;

    @Schema(description = "ID del trabajador seleccionado", example = "1")
    @NotNull(message = "El trabajadorId es obligatorio")
    private Long trabajadorId;

    @Schema(description = "ID del servicio a agendar", example = "2")
    @NotNull(message = "El servicioId es obligatorio")
    private Long servicioId;

    @Schema(description = "Fecha y hora de inicio de la cita", example = "2026-10-15T10:00:00")
    @NotNull(message = "La fecha y hora de inicio es obligatoria")
    private LocalDateTime fechaHoraInicio;

    @Schema(description = "Notas adicionales ingresadas por el cliente", example = "Primera sesión")
    private String notasCliente;

    @Schema(description = "Estado inicial de la cita (opcional, por defecto CONFIRMADA)", example = "PENDIENTE")
    private EstadoCita estado;

    public CitaCreateDTO() {
    }

    public CitaCreateDTO(Long clienteId, Long trabajadorId, Long servicioId, LocalDateTime fechaHoraInicio, String notasCliente) {
        this.clienteId = clienteId;
        this.trabajadorId = trabajadorId;
        this.servicioId = servicioId;
        this.fechaHoraInicio = fechaHoraInicio;
        this.notasCliente = notasCliente;
    }

    public CitaCreateDTO(Long clienteId, Long trabajadorId, Long servicioId, LocalDateTime fechaHoraInicio, String notasCliente, EstadoCita estado) {
        this.clienteId = clienteId;
        this.trabajadorId = trabajadorId;
        this.servicioId = servicioId;
        this.fechaHoraInicio = fechaHoraInicio;
        this.notasCliente = notasCliente;
        this.estado = estado;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public Long getTrabajadorId() {
        return trabajadorId;
    }

    public void setTrabajadorId(Long trabajadorId) {
        this.trabajadorId = trabajadorId;
    }

    public Long getServicioId() {
        return servicioId;
    }

    public void setServicioId(Long servicioId) {
        this.servicioId = servicioId;
    }

    public LocalDateTime getFechaHoraInicio() {
        return fechaHoraInicio;
    }

    public void setFechaHoraInicio(LocalDateTime fechaHoraInicio) {
        this.fechaHoraInicio = fechaHoraInicio;
    }

    public String getNotasCliente() {
        return notasCliente;
    }

    public void setNotasCliente(String notasCliente) {
        this.notasCliente = notasCliente;
    }

    public EstadoCita getEstado() {
        return estado;
    }

    public void setEstado(EstadoCita estado) {
        this.estado = estado;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CitaCreateDTO that)) return false;
        return Objects.equals(clienteId, that.clienteId)
                && Objects.equals(trabajadorId, that.trabajadorId)
                && Objects.equals(servicioId, that.servicioId)
                && Objects.equals(fechaHoraInicio, that.fechaHoraInicio)
                && Objects.equals(notasCliente, that.notasCliente)
                && estado == that.estado;
    }

    @Override
    public int hashCode() {
        return Objects.hash(clienteId, trabajadorId, servicioId, fechaHoraInicio, notasCliente, estado);
    }
}
