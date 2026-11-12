package cl.nicolet.backend.security;

import cl.nicolet.backend.model.Usuario;
import cl.nicolet.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String correo) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByCorreoIgnoreCase(correo)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con correo: " + correo));
        if (usuario.getEstado() != Usuario.ESTADO_ACTIVO) {
            throw new UsernameNotFoundException("Credenciales inválidas");
        }

        return UserDetailsImpl.build(usuario);
    }

    /** Busca al usuario por su ID para validar el token JWT y rechaza cuentas desactivadas. */
    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
        if (usuario.getEstado() != Usuario.ESTADO_ACTIVO) {
            throw new UsernameNotFoundException("Credenciales inválidas");
        }
        return UserDetailsImpl.build(usuario);
    }
}
