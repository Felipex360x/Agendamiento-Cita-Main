package cl.nicolet.backend.security;

import cl.nicolet.backend.model.Usuario;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Getter
@AllArgsConstructor
public class UserDetailsImpl implements UserDetails {

    private final Long id;
    private final String nombre;
    private final String apellidoP;
    private final String correo;

    @JsonIgnore
    private final String password;

    private final Collection<? extends GrantedAuthority> authorities;

    public static UserDetailsImpl build(Usuario usuario) {
        String rolNombre = "CLIENTE";
        if (usuario.getTipoUsuario() != null && usuario.getTipoUsuario().getNombre() != null) {
            rolNombre = usuario.getTipoUsuario().getNombre().toUpperCase();
        }

        String roleAuthority = rolNombre.startsWith("ROLE_") ? rolNombre : "ROLE_" + rolNombre;
        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority(roleAuthority));

        return new UserDetailsImpl(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellidoP(),
                usuario.getCorreo(),
                usuario.getPassword(),
                authorities
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return correo;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
