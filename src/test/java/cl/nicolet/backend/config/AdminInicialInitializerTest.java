package cl.nicolet.backend.config;

import cl.nicolet.backend.model.TipoUsuario;
import cl.nicolet.backend.model.Usuario;
import cl.nicolet.backend.repository.TipoUsuarioRepository;
import cl.nicolet.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminInicialInitializerTest {

    private final UsuarioRepository usuarios = mock(UsuarioRepository.class);
    private final TipoUsuarioRepository tipos = mock(TipoUsuarioRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

    @BeforeEach
    void rolExistente() {
        when(tipos.findByNombreIgnoreCase("ADMINISTRADOR"))
                .thenReturn(Optional.of(new TipoUsuario(1L, "ADMINISTRADOR", "Admin")));
    }

    private Usuario guardado() {
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarios).save(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("Si ya hay un administrador no crea ni modifica ninguna cuenta")
    void noTocaBasesConAdministrador() {
        when(usuarios.existsByTipoUsuarioNombreIgnoreCase("ADMINISTRADOR")).thenReturn(true);

        new AdminInicialInitializer(usuarios, tipos, encoder, "jefa@estudio.test", "Clave-Larga-123").run();

        verify(usuarios, never()).save(any());
    }

    @Test
    @DisplayName("Sin administradores crea uno con el correo y la contraseña configurados, guardada como hash")
    void creaElAdministradorConfigurado() {
        new AdminInicialInitializer(usuarios, tipos, encoder, "  Jefa@Estudio.test ", "Clave-Larga-123").run();

        Usuario admin = guardado();
        assertThat(admin.getCorreo()).isEqualTo("jefa@estudio.test");
        assertThat(admin.getTipoUsuario().getNombre()).isEqualTo("ADMINISTRADOR");
        assertThat(admin.getPassword()).startsWith("$2").isNotEqualTo("Clave-Larga-123");
        assertThat(encoder.matches("Clave-Larga-123", admin.getPassword())).isTrue();
    }

    @Test
    @DisplayName("Sin contraseña configurada genera una al azar distinta en cada base")
    void generaContrasenaAleatoria() {
        new AdminInicialInitializer(usuarios, tipos, encoder, "", "").run();
        Usuario primero = guardado();

        UsuarioRepository otraBase = mock(UsuarioRepository.class);
        new AdminInicialInitializer(otraBase, tipos, encoder, "", "").run();
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(otraBase).save(captor.capture());

        assertThat(primero.getCorreo()).isEqualTo("admin@nicolet.cl");
        assertThat(primero.getPassword()).startsWith("$2");
        for (String conocida : new String[]{"", "admin", "123456", "Admin123!", "admin@nicolet.cl"}) {
            assertThat(encoder.matches(conocida, primero.getPassword())).isFalse();
        }
        assertThat(primero.getPassword()).isNotEqualTo(captor.getValue().getPassword());
    }

    @Test
    @DisplayName("No convierte en administrador una cuenta existente que ya usa ese correo")
    void noReutilizaUnCorreoExistente() {
        when(usuarios.existsByCorreoIgnoreCase("clienta@estudio.test")).thenReturn(true);

        new AdminInicialInitializer(usuarios, tipos, encoder, "clienta@estudio.test", "Clave-Larga-123").run();

        verify(usuarios, never()).save(any());
    }

    @Test
    @DisplayName("Rechaza una contraseña que BCrypt truncaría (más de 72 bytes)")
    void rechazaContrasenaDemasiadoLarga() {
        new AdminInicialInitializer(usuarios, tipos, encoder, "", "x".repeat(73)).run();

        verify(usuarios, never()).save(any());
    }

    @Test
    @DisplayName("Restablece la contraseña del administrador existente si reset-password está activo")
    void restableceContrasenaDeAdminExistenteSiSeActivaReset() {
        when(usuarios.existsByTipoUsuarioNombreIgnoreCase("ADMINISTRADOR")).thenReturn(true);
        Usuario adminExistente = new Usuario();
        adminExistente.setCorreo("admin@nicolet.cl");
        adminExistente.setPassword("antigua_hash");
        when(usuarios.findByCorreoIgnoreCase("admin@nicolet.cl")).thenReturn(Optional.of(adminExistente));

        new AdminInicialInitializer(usuarios, tipos, encoder, "admin@nicolet.cl", "NuevaClave-2026!", true).run();

        assertThat(encoder.matches("NuevaClave-2026!", adminExistente.getPassword())).isTrue();
        verify(usuarios).save(adminExistente);
    }
}
