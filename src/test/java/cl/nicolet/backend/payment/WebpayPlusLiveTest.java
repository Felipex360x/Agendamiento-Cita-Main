package cl.nicolet.backend.payment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Prueba opcional contra el ambiente de INTEGRACIÓN real de Transbank (credenciales públicas de pruebas).
 * No corre por defecto. Para ejecutarla: {@code ./mvnw test -Dtest=WebpayPlusLiveTest -Dwebpay.live=true}
 */
@EnabledIfSystemProperty(named = "webpay.live", matches = "true")
class WebpayPlusLiveTest {

    @Test
    void creaYConsultaUnaTransaccionEnIntegracion() {
        WebpayPlusClient cliente = new WebpayPlusClient("INTEGRATION", "597055555532",
                "579B532A7440BB0C9079DED94D31EA1615BACEB56610332264630D42D0A36B1C", 5000, 15000);
        String orden = "LIVE-" + System.currentTimeMillis();

        WebpayPlusClient.CrearRespuesta creada = cliente.crear(orden, "sesion-" + orden, 1000, "http://localhost:8080/retorno");

        assertThat(creada.token()).hasSize(64);
        assertThat(creada.url()).startsWith("https://webpay3gint.transbank.cl/");
        assertThat(cliente.consultar(creada.token()).status()).isEqualTo("INITIALIZED");
    }
}
