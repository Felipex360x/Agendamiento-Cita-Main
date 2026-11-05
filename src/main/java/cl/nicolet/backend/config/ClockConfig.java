package cl.nicolet.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * Reloj único de la aplicación. Contrato de fechas: {@code LocalDate}/{@code LocalDateTime} de la API son
 * "hora de pared" del negocio, interpretadas en la zona {@code app.timezone} (por defecto America/Santiago),
 * nunca en la zona del servidor ni la del navegador. «Hoy» y «hora pasada» se calculan con este reloj.
 * Si {@code app.timezone} está vacío se usa la zona del sistema (útil en pruebas locales).
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${app.timezone:America/Santiago}") String timezone) {
        ZoneId zona = (timezone == null || timezone.isBlank()) ? ZoneId.systemDefault() : ZoneId.of(timezone.trim());
        return Clock.system(zona);
    }
}
