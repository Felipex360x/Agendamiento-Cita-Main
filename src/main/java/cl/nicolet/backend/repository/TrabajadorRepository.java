package cl.nicolet.backend.repository;

import cl.nicolet.backend.model.Trabajador;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrabajadorRepository extends JpaRepository<Trabajador, Long> {
    /**
     * Bloquea la fila del profesional hasta el fin de la transacción. Serializa las operaciones que ocupan
     * o liberan su agenda sin bloquear a los demás profesionales.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Trabajador t WHERE t.id = :id")
    Optional<Trabajador> findByIdForUpdate(@Param("id") Long id);

    Optional<Trabajador> findByUsuarioId(Long usuarioId);
    Optional<Trabajador> findByRutDni(String rutDni);
    List<Trabajador> findByActivoTrue();
    boolean existsByRutDni(String rutDni);
    boolean existsByUsuarioId(Long usuarioId);
}
