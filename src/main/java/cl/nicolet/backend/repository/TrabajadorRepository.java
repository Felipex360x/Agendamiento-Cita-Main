package cl.nicolet.backend.repository;

import cl.nicolet.backend.model.Trabajador;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrabajadorRepository extends JpaRepository<Trabajador, Long> {
    Optional<Trabajador> findByUsuarioId(Long usuarioId);
    Optional<Trabajador> findByRutDni(String rutDni);
    List<Trabajador> findByActivoTrue();
    boolean existsByRutDni(String rutDni);
    boolean existsByUsuarioId(Long usuarioId);
}
