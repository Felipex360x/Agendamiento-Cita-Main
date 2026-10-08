package cl.nicolet.backend.repository;

import cl.nicolet.backend.model.TipoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TipoUsuarioRepository extends JpaRepository<TipoUsuario, Long> {

    Optional<TipoUsuario> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

}
