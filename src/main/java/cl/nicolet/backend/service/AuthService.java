package cl.nicolet.backend.service;

import cl.nicolet.backend.dto.RegistroRequestDTO;
import cl.nicolet.backend.exception.ConflictoException;
import cl.nicolet.backend.model.TipoUsuario;
import cl.nicolet.backend.model.Usuario;
import cl.nicolet.backend.repository.TipoUsuarioRepository;
import cl.nicolet.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

/** Orquesta el alta de cuenta y perfil de cliente en un único límite transaccional. */
@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final TipoUsuarioRepository tipoUsuarioRepository;
    private final PerfilUsuarioService perfilUsuarioService;
    private final PasswordEncoder passwordEncoder;

    @Autowired(required = false)
    private EmailService emailService;

    public AuthService(UsuarioRepository usuarioRepository,
                       TipoUsuarioRepository tipoUsuarioRepository,
                       PerfilUsuarioService perfilUsuarioService,
                       PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.tipoUsuarioRepository = tipoUsuarioRepository;
        this.perfilUsuarioService = perfilUsuarioService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Crea Usuario + Cliente de forma atómica: si falla cualquier paso se revierte todo, de modo que no
     * quedan cuentas huérfanas ni un correo ocupado por un alta fallida.
     *
     * @throws ConflictoException si el correo ya existe (también ante duplicados concurrentes)
     */
    @Transactional
    public Usuario registrarCliente(RegistroRequestDTO request) {
        String correo = request.getCorreo().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
            throw new ConflictoException("El correo electrónico ya se encuentra registrado.");
        }

        TipoUsuario tipoCliente = tipoUsuarioRepository.findByNombreIgnoreCase("CLIENTE")
                .orElseGet(() -> {
                    TipoUsuario nuevo = new TipoUsuario();
                    nuevo.setNombre("CLIENTE");
                    nuevo.setDescripcion("Cliente que solicita y agenda citas");
                    return tipoUsuarioRepository.save(nuevo);
                });

        Usuario usuario = new Usuario();
        usuario.setNombre(request.getNombre().trim());
        usuario.setApellidoP(request.getApellidoP().trim());
        usuario.setCorreo(correo);
        usuario.setPassword(passwordEncoder.encode(request.getPassword()));
        usuario.setTipoUsuario(tipoCliente);

        String telefono = request.getTelefono() != null && !request.getTelefono().isBlank()
                ? request.getTelefono().trim()
                : null;
        Usuario guardado;
        try {
            guardado = usuarioRepository.saveAndFlush(usuario);
        } catch (DataIntegrityViolationException e) {
            // Duplicado concurrente: el índice único de correo rechazó el INSERT.
            if (usuarioRepository.existsByCorreoIgnoreCase(correo)) {
                throw new ConflictoException("El correo electrónico ya se encuentra registrado.");
            }
            throw e;
        }
        // Un fallo aquí NO se interpreta como correo duplicado: propaga y revierte toda la transacción.
        perfilUsuarioService.crearPerfilCliente(guardado, telefono);
        if (emailService != null) {
            try {
                emailService.enviarBienvenida(guardado);
            } catch (Exception ignored) {
                // El envío de correo no debe interrumpir el registro del cliente
            }
        }
        return guardado;
    }
}
