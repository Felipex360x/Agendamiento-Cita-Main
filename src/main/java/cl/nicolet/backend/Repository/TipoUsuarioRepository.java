package cl.nicolet.backend.Repository;

import cl.nicolet.backend.Model.TipoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TipoUsuarioRepository extends JpaRepository<TipoUsuario, Long> {

    Optional<TipoUsuario> findByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCase(String nombre);

}
