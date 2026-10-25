package cl.nicolet.backend.dto;

import java.time.LocalTime;

public class HorarioDisponibilidadDTO {
    private Integer diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private LocalTime horaInicioDescanso;
    private LocalTime horaFinDescanso;
    private Boolean activo;

    public HorarioDisponibilidadDTO() { }

    public HorarioDisponibilidadDTO(Integer diaSemana, LocalTime horaInicio, LocalTime horaFin,
                                    LocalTime horaInicioDescanso, LocalTime horaFinDescanso, Boolean activo) {
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.horaInicioDescanso = horaInicioDescanso;
        this.horaFinDescanso = horaFinDescanso;
        this.activo = activo;
    }

    public Integer getDiaSemana() { return diaSemana; }
    public void setDiaSemana(Integer diaSemana) { this.diaSemana = diaSemana; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }
    public LocalTime getHoraInicioDescanso() { return horaInicioDescanso; }
    public void setHoraInicioDescanso(LocalTime horaInicioDescanso) { this.horaInicioDescanso = horaInicioDescanso; }
    public LocalTime getHoraFinDescanso() { return horaFinDescanso; }
    public void setHoraFinDescanso(LocalTime horaFinDescanso) { this.horaFinDescanso = horaFinDescanso; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
