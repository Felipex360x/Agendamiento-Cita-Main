package cl.nicolet.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Reloj principal del sistema. Usa la zona horaria del negocio (por defecto America/Santiago)
 * para calcular la fecha y hora actual. Si no se indica una zona, usa la del sistema.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${app.timezone:America/Santiago}") String timezone) {
        ZoneId zona = (timezone == null || timezone.isBlank()) ? ZoneId.systemDefault() : ZoneId.of(timezone.trim());
        return Clock.system(zona);
    }
}
