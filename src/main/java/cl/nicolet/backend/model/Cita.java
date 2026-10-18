package cl.nicolet.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "cita")
public class Cita {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "codigo_reserva", nullable = false, unique = true, length = 20)
    private String codigoReserva;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;

    @ManyToOne(optional = false)
    @JoinColumn(name = "trabajador_id", nullable = false)
    private Trabajador trabajador;

    @ManyToOne(optional = false)
    @JoinColumn(name = "servicio_id", nullable = false)
    private Servicio servicio;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private LocalDateTime fechaHoraInicio;

    @Column(name = "fecha_hora_fin", nullable = false)
    private LocalDateTime fechaHoraFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoCita estado = EstadoCita.PENDIENTE;

    @Column(name = "precio_final", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioFinal;

    @Column(name = "notas_cliente", columnDefinition = "TEXT")
    private String notasCliente;

    @Column(name = "notas_trabajador", columnDefinition = "TEXT")
    private String notasTrabajador;

    @Column(name = "motivo_cancelacion")
    private String motivoCancelacion;

    public Cita() {
    }

    public Cita(Long id, String codigoReserva, Cliente cliente, Trabajador trabajador, Servicio servicio,
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

    public Cliente getCliente() {
        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
    }

    public Trabajador getTrabajador() {
        return trabajador;
    }

    public void setTrabajador(Trabajador trabajador) {
        this.trabajador = trabajador;
    }

    public Servicio getServicio() {
        return servicio;
    }

    public void setServicio(Servicio servicio) {
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
        if (!(o instanceof Cita that)) return false;
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

    @Override
    public String toString() {
        return "Cita(id=" + id + ", codigoReserva=" + codigoReserva + ", estado=" + estado
                + ", fechaHoraInicio=" + fechaHoraInicio + ")";
    }
}
