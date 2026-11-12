package cl.nicolet.backend.config;

import cl.nicolet.backend.model.TipoUsuario;
import cl.nicolet.backend.model.Usuario;
import cl.nicolet.backend.repository.TipoUsuarioRepository;
import cl.nicolet.backend.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Crea el primer administrador cuando la base no tiene ninguno, para que el repositorio
 * no tenga que traer cuentas con contraseñas conocidas. Si ya existe un administrador
 * no hace nada: nunca modifica ni restablece cuentas existentes.
 */
@Component
@Order(2)
public class AdminInicialInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminInicialInitializer.class);
    private static final String ROL_ADMIN = "ADMINISTRADOR";
    private static final String CORREO_POR_DEFECTO = "admin@nicolet.cl";

    private final UsuarioRepository usuarioRepository;
    private final TipoUsuarioRepository tipoUsuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String correoConfigurado;
    private final String passwordConfigurada;

    public AdminInicialInitializer(UsuarioRepository usuarioRepository,
                                   TipoUsuarioRepository tipoUsuarioRepository,
                                   PasswordEncoder passwordEncoder,
                                   @Value("${app.admin.email:}") String correoConfigurado,
                                   @Value("${app.admin.password:}") String passwordConfigurada) {
        this.usuarioRepository = usuarioRepository;
        this.tipoUsuarioRepository = tipoUsuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.correoConfigurado = correoConfigurado;
        this.passwordConfigurada = passwordConfigurada;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarioRepository.existsByTipoUsuarioNombreIgnoreCase(ROL_ADMIN)) {
            return;
        }
        String correo = correoConfigurado == null || correoConfigurado.isBlank()
                ? CORREO_POR_DEFECTO
                : correoConfigurado.trim().toLowerCase();
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            log.warn("No hay administradores y el correo configurado en APP_ADMIN_EMAIL ya pertenece a otra cuenta; "
                    + "no se creó el administrador inicial. Define otro correo en APP_ADMIN_EMAIL.");
            return;
        }

        boolean generada = passwordConfigurada == null || passwordConfigurada.isBlank();
        String password = generada ? generarPassword() : passwordConfigurada;
        if (password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) {
            log.error("APP_ADMIN_PASSWORD supera los 72 bytes que admite BCrypt; no se creó el administrador inicial.");
            return;
        }

        TipoUsuario rol = tipoUsuarioRepository.findByNombreIgnoreCase(ROL_ADMIN).orElseGet(() -> {
            TipoUsuario nuevo = new TipoUsuario();
            nuevo.setNombre(ROL_ADMIN);
            nuevo.setDescripcion("Administrador con acceso total al sistema");
            return tipoUsuarioRepository.save(nuevo);
        });

        Usuario admin = new Usuario();
        admin.setNombre("Administrador");
        admin.setApellidoP("Inicial");
        admin.setCorreo(correo);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setTipoUsuario(rol);
        usuarioRepository.save(admin);

        if (generada) {
            log.warn("Se creó el administrador inicial {} con una contraseña generada: {}  "
                    + "Guárdala y cámbiala desde el panel de usuarios; no volverá a mostrarse.", correo, password);
        } else {
            log.info("Se creó el administrador inicial {} con la contraseña de APP_ADMIN_PASSWORD.", correo);
        }
    }

    private static String generarPassword() {
        byte[] azar = new byte[18];
        new SecureRandom().nextBytes(azar);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(azar);
    }
}
