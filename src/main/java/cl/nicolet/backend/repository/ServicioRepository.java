package cl.nicolet.backend.repository;

import cl.nicolet.backend.model.Servicio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServicioRepository extends JpaRepository<Servicio, Long> {
    Optional<Servicio> findByNombreIgnoreCase(String nombre);
    List<Servicio> findByActivoTrue();
    boolean existsByNombreIgnoreCase(String nombre);
}
