package cl.nicolet.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalTime;
import java.util.Objects;

@Entity
@Table(name = "horario_disponibilidad")
public class HorarioDisponibilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "trabajador_id", nullable = false)
    private Trabajador trabajador;

    @Column(name = "dia_semana", nullable = false)
    private Integer diaSemana; // 1=Lunes, 7=Domingo

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(name = "hora_inicio_descanso")
    private LocalTime horaInicioDescanso;

    @Column(name = "hora_fin_descanso")
    private LocalTime horaFinDescanso;

    @Column(nullable = false)
    private Boolean activo = true;

    public HorarioDisponibilidad() {
    }

    public HorarioDisponibilidad(Long id, Trabajador trabajador, Integer diaSemana, LocalTime horaInicio,
                                 LocalTime horaFin, LocalTime horaInicioDescanso, LocalTime horaFinDescanso, Boolean activo) {
        this.id = id;
        this.trabajador = trabajador;
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.horaInicioDescanso = horaInicioDescanso;
        this.horaFinDescanso = horaFinDescanso;
        this.activo = activo;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Trabajador getTrabajador() {
        return trabajador;
    }

    public void setTrabajador(Trabajador trabajador) {
        this.trabajador = trabajador;
    }

    public Integer getDiaSemana() {
        return diaSemana;
    }

    public void setDiaSemana(Integer diaSemana) {
        this.diaSemana = diaSemana;
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

    public LocalTime getHoraInicioDescanso() {
        return horaInicioDescanso;
    }

    public void setHoraInicioDescanso(LocalTime horaInicioDescanso) {
        this.horaInicioDescanso = horaInicioDescanso;
    }

    public LocalTime getHoraFinDescanso() {
        return horaFinDescanso;
    }

    public void setHoraFinDescanso(LocalTime horaFinDescanso) {
        this.horaFinDescanso = horaFinDescanso;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HorarioDisponibilidad that)) return false;
        return Objects.equals(id, that.id)
                && Objects.equals(trabajador, that.trabajador)
                && Objects.equals(diaSemana, that.diaSemana)
                && Objects.equals(horaInicio, that.horaInicio)
                && Objects.equals(horaFin, that.horaFin)
                && Objects.equals(horaInicioDescanso, that.horaInicioDescanso)
                && Objects.equals(horaFinDescanso, that.horaFinDescanso)
                && Objects.equals(activo, that.activo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, trabajador, diaSemana, horaInicio, horaFin, horaInicioDescanso, horaFinDescanso, activo);
    }

    @Override
    public String toString() {
        return "HorarioDisponibilidad(id=" + id + ", diaSemana=" + diaSemana
                + ", horaInicio=" + horaInicio + ", horaFin=" + horaFin + ", activo=" + activo + ")";
    }
}
