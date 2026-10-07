package cl.nicolet.backend.Repository;

import cl.nicolet.backend.Model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByCorreoIgnoreCase(String correo);

}