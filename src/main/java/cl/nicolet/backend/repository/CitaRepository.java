package cl.nicolet.backend.repository;

import cl.nicolet.backend.model.Cita;
import cl.nicolet.backend.model.EstadoCita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CitaRepository extends JpaRepository<Cita, Long> {

    Optional<Cita> findByCodigoReserva(String codigoReserva);

    List<Cita> findByClienteId(Long clienteId);

    List<Cita> findByTrabajadorId(Long trabajadorId);

    List<Cita> findByEstado(EstadoCita estado);

    @Query("SELECT c FROM Cita c WHERE c.trabajador.id = :trabajadorId " +
           "AND c.fechaHoraInicio >= :inicio AND c.fechaHoraInicio <= :fin ORDER BY c.fechaHoraInicio ASC")
    List<Cita> findByTrabajadorAndRangoFechas(
        @Param("trabajadorId") Long trabajadorId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin
    );

    @Query("SELECT COUNT(c) > 0 FROM Cita c " +
           "WHERE c.trabajador.id = :trabajadorId " +
           "AND c.estado <> cl.nicolet.backend.model.EstadoCita.CANCELADA " +
           "AND (:inicio < c.fechaHoraFin AND :fin > c.fechaHoraInicio)")
    boolean existeSolapamiento(
        @Param("trabajadorId") Long trabajadorId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin
    );

    @Query("SELECT COUNT(c) > 0 FROM Cita c " +
           "WHERE c.trabajador.id = :trabajadorId " +
           "AND c.id <> :citaId " +
           "AND c.estado <> cl.nicolet.backend.model.EstadoCita.CANCELADA " +
           "AND (:inicio < c.fechaHoraFin AND :fin > c.fechaHoraInicio)")
    boolean existeSolapamientoExcluyendoCita(
        @Param("trabajadorId") Long trabajadorId,
        @Param("citaId") Long citaId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin
    );
}
