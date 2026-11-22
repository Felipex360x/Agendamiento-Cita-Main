package cl.nicolet.backend.repository;

import cl.nicolet.backend.model.EstadoTransaccionPago;
import cl.nicolet.backend.model.Pago;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagoRepository extends JpaRepository<Pago, Long> {

    Optional<Pago> findByToken(String token);

    Optional<Pago> findByBuyOrder(String buyOrder);

    long countByCitaId(Long citaId);

    List<Pago> findByCitaIdOrderByIdAsc(Long citaId);

    List<Pago> findByCitaIdAndEstado(Long citaId, EstadoTransaccionPago estado);

    /** Solo el id de la cita, para poder bloquearla antes de leer el pago. */
    @Query("SELECT p.cita.id FROM Pago p WHERE p.token = :token")
    Optional<Long> findCitaIdByToken(@Param("token") String token);

    @Query("SELECT p.cita.id FROM Pago p WHERE p.buyOrder = :buyOrder")
    Optional<Long> findCitaIdByBuyOrder(@Param("buyOrder") String buyOrder);

    /** Historial para administración: pagos creados en el rango, con la cita y las personas ya cargadas. */
    @Query("SELECT p FROM Pago p JOIN FETCH p.cita c JOIN FETCH c.cliente cl JOIN FETCH cl.usuario "
            + "JOIN FETCH c.trabajador t JOIN FETCH t.usuario JOIN FETCH c.servicio "
            + "WHERE p.creadoEn >= :desde AND p.creadoEn < :hasta AND (:estado IS NULL OR p.estado = :estado) "
            + "ORDER BY p.creadoEn DESC, p.id DESC")
    List<Pago> historial(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta,
                         @Param("estado") EstadoTransaccionPago estado, Pageable pageable);
}
