package cl.nicolet.backend.repository;

import cl.nicolet.backend.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    Optional<Cliente> findByUsuarioId(Long usuarioId);
    Optional<Cliente> findByRutDni(String rutDni);
    boolean existsByRutDni(String rutDni);
    boolean existsByUsuarioId(Long usuarioId);
}
