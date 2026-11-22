package cl.nicolet.backend.repository;

import cl.nicolet.backend.model.Cita;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    @Query("SELECT COUNT(c) > 0 FROM Cita c " +
           "WHERE c.trabajador.id = :trabajadorId " +
           "AND c.estado <> cl.nicolet.backend.model.EstadoCita.CANCELADA " +
           "AND (:inicio < c.fechaHoraFin AND :fin > c.fechaHoraInicio)")
    boolean existeSolapamiento(
        @Param("trabajadorId") Long trabajadorId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin
    );

    /** Busca citas no canceladas que se cruzan con el horario indicado. */
    @Query("SELECT c FROM Cita c WHERE c.trabajador.id = :trabajadorId " +
           "AND c.estado <> cl.nicolet.backend.model.EstadoCita.CANCELADA " +
           "AND c.fechaHoraInicio < :fin AND c.fechaHoraFin > :inicio ORDER BY c.fechaHoraInicio ASC")
    List<Cita> findSolapadasConRango(
        @Param("trabajadorId") Long trabajadorId,
        @Param("inicio") LocalDateTime inicio,
        @Param("fin") LocalDateTime fin
    );

    /** Busca citas activas que todavía no terminan a partir de la fecha indicada. */
    @Query("SELECT c FROM Cita c WHERE c.trabajador.id = :trabajadorId " +
           "AND c.estado IN (cl.nicolet.backend.model.EstadoCita.PENDIENTE, cl.nicolet.backend.model.EstadoCita.CONFIRMADA, cl.nicolet.backend.model.EstadoCita.EN_ATENCION) " +
           "AND c.fechaHoraFin > :desde ORDER BY c.fechaHoraInicio ASC")
    List<Cita> findVigentesDesde(@Param("trabajadorId") Long trabajadorId, @Param("desde") LocalDateTime desde);

    /** Bloquea la cita mientras se confirma o vence su pago, para que no se hagan las dos cosas a la vez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cita c WHERE c.id = :id")
    Optional<Cita> findByIdForUpdate(@Param("id") Long id);

    /** Ids de citas que esperan pago y ya pasaron su plazo. */
    @Query("SELECT c.id FROM Cita c WHERE c.estadoPago = cl.nicolet.backend.model.EstadoPago.PENDIENTE_PAGO " +
           "AND c.estado = cl.nicolet.backend.model.EstadoCita.PENDIENTE AND c.pagoExpiraEn < :ahora")
    List<Long> findIdsConPagoVencido(@Param("ahora") LocalDateTime ahora);
}
