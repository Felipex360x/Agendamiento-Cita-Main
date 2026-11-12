package cl.nicolet.backend.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Optional;

@Component
public class JwtUtils {

    private static final Logger log = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    /** Versión actual del token. Los tokens antiguos sin versión se rechazan. */
    public static final int TOKEN_VERSION = 2;
    private static final String CLAIM_VERSION = "ver";

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }

    public String generarToken(UserDetailsImpl userDetails) {
        String rol = userDetails.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority())
                .orElse("SIN_ROL");

        return Jwts.builder()
                .subject(String.valueOf(userDetails.getId()))
                .claim(CLAIM_VERSION, TOKEN_VERSION)
                .claim("id", userDetails.getId())
                .claim("nombre", userDetails.getNombre())
                .claim("apellido", userDetails.getApellidoP())
                .claim("rol", rol)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Obtiene el ID del usuario desde el token. Devuelve vacío si el token no sirve, venció
     * o tiene formato antiguo. Los permisos reales siempre se consultan en la base de datos.
     */
    public Optional<Long> getUserIdFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            Integer version = claims.get(CLAIM_VERSION, Integer.class);
            if (version == null || version != TOKEN_VERSION) {
                log.debug("Token JWT rechazado (formato anterior o versión desconocida)");
                return Optional.empty();
            }
            long subjectId = Long.parseLong(claims.getSubject());
            Number idClaim = claims.get("id", Number.class);
            if (idClaim == null || idClaim.longValue() != subjectId) {
                log.debug("Token JWT rechazado (identidad discordante)");
                return Optional.empty();
            }
            return Optional.of(subjectId);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Token JWT rechazado ({})", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validarToken(String token) {
        try {
            
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // No mostramos el contenido del token en el log si falla la validación.
            log.debug("Token JWT rechazado ({})", e.getClass().getSimpleName());
        }
        return false;
    }
}
