package cl.nicolet.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Libera los horarios de las citas cuyo pago en línea no se completó a tiempo. */
@Component
public class PagoExpiracionJob {

    private static final Logger log = LoggerFactory.getLogger(PagoExpiracionJob.class);

    private final PagoService pagoService;

    public PagoExpiracionJob(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @Scheduled(fixedDelayString = "${webpay.expiration-check-ms:60000}", initialDelayString = "${webpay.expiration-check-ms:60000}")
    public void expirarPagosVencidos() {
        try {
            pagoService.expirarPagosVencidos();
        } catch (RuntimeException e) {
            log.warn("Falló la revisión de pagos vencidos: {}", e.getMessage());
        }
    }
}
