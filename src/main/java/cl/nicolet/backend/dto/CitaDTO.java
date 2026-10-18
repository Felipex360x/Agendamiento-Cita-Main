package cl.nicolet.backend.dto;

import cl.nicolet.backend.model.EstadoCita;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class CitaDTO {
    private Long id;
    private String codigoReserva;
    private ClienteDTO cliente;
    private TrabajadorDTO trabajador;
    private ServicioDTO servicio;
    private LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;
    private EstadoCita estado;
    private BigDecimal precioFinal;
    private String notasCliente;
    private String notasTrabajador;
    private String motivoCancelacion;

    public CitaDTO() {
    }

    public CitaDTO(Long id, String codigoReserva, ClienteDTO cliente, TrabajadorDTO trabajador, ServicioDTO servicio,
                   LocalDateTime fechaHoraInicio, LocalDateTime fechaHoraFin, EstadoCita estado,
                   BigDecimal precioFinal, String notasCliente, String notasTrabajador, String motivoCancelacion) {
        this.id = id;
        this.codigoReserva = codigoReserva;
        this.cliente = cliente;
        this.trabajador = trabajador;
        this.servicio = servicio;
        this.fechaHoraInicio = fechaHoraInicio;
        this.fechaHoraFin = fechaHoraFin;
        this.estado = estado;
        this.precioFinal = precioFinal;
        this.notasCliente = notasCliente;
        this.notasTrabajador = notasTrabajador;
        this.motivoCancelacion = motivoCancelacion;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCodigoReserva() {
        return codigoReserva;
    }

    public void setCodigoReserva(String codigoReserva) {
        this.codigoReserva = codigoReserva;
    }

    public ClienteDTO getCliente() {
        return cliente;
    }

    public void setCliente(ClienteDTO cliente) {
        this.cliente = cliente;
    }

    public TrabajadorDTO getTrabajador() {
        return trabajador;
    }

    public void setTrabajador(TrabajadorDTO trabajador) {
        this.trabajador = trabajador;
    }

    public ServicioDTO getServicio() {
        return servicio;
    }

    public void setServicio(ServicioDTO servicio) {
        this.servicio = servicio;
    }

    public LocalDateTime getFechaHoraInicio() {
        return fechaHoraInicio;
    }

    public void setFechaHoraInicio(LocalDateTime fechaHoraInicio) {
        this.fechaHoraInicio = fechaHoraInicio;
    }

    public LocalDateTime getFechaHoraFin() {
        return fechaHoraFin;
    }

    public void setFechaHoraFin(LocalDateTime fechaHoraFin) {
        this.fechaHoraFin = fechaHoraFin;
    }

    public EstadoCita getEstado() {
        return estado;
    }

    public void setEstado(EstadoCita estado) {
        this.estado = estado;
    }

    public BigDecimal getPrecioFinal() {
        return precioFinal;
    }

    public void setPrecioFinal(BigDecimal precioFinal) {
        this.precioFinal = precioFinal;
    }

    public String getNotasCliente() {
        return notasCliente;
    }

    public void setNotasCliente(String notasCliente) {
        this.notasCliente = notasCliente;
    }

    public String getNotasTrabajador() {
        return notasTrabajador;
    }

    public void setNotasTrabajador(String notasTrabajador) {
        this.notasTrabajador = notasTrabajador;
    }

    public String getMotivoCancelacion() {
        return motivoCancelacion;
    }

    public void setMotivoCancelacion(String motivoCancelacion) {
        this.motivoCancelacion = motivoCancelacion;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CitaDTO that)) return false;
        return Objects.equals(id, that.id)
                && Objects.equals(codigoReserva, that.codigoReserva)
                && Objects.equals(cliente, that.cliente)
                && Objects.equals(trabajador, that.trabajador)
                && Objects.equals(servicio, that.servicio)
                && Objects.equals(fechaHoraInicio, that.fechaHoraInicio)
                && Objects.equals(fechaHoraFin, that.fechaHoraFin)
                && estado == that.estado
                && Objects.equals(precioFinal, that.precioFinal)
                && Objects.equals(notasCliente, that.notasCliente)
                && Objects.equals(notasTrabajador, that.notasTrabajador)
                && Objects.equals(motivoCancelacion, that.motivoCancelacion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, codigoReserva, cliente, trabajador, servicio, fechaHoraInicio, fechaHoraFin,
                estado, precioFinal, notasCliente, notasTrabajador, motivoCancelacion);
    }
}
