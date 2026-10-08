package cl.nicolet.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Data
@Entity
@Table(name = "horario_disponibilidad")
@NoArgsConstructor
@AllArgsConstructor
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
}
