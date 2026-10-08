package cl.nicolet.backend.repository;

import cl.nicolet.backend.model.HorarioDisponibilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HorarioDisponibilidadRepository extends JpaRepository<HorarioDisponibilidad, Long> {
    List<HorarioDisponibilidad> findByTrabajadorIdAndActivoTrue(Long trabajadorId);
    Optional<HorarioDisponibilidad> findByTrabajadorIdAndDiaSemanaAndActivoTrue(Long trabajadorId, Integer diaSemana);
}
