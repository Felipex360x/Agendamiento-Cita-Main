package cl.nicolet.backend.payment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

/** Pruebas del cliente de Webpay Plus con un servidor simulado (no llama a Transbank). */
class WebpayPlusClientTest {

    private static final String BASE = WebpayPlusClient.HOST_INTEGRACION + WebpayPlusClient.RUTA_BASE;

    private MockRestServiceServer servidor;
    private WebpayPlusClient cliente;

    @BeforeEach
    void preparar() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new WebpayPlusClient(builder.build());
    }

    @Test
    @DisplayName("crear envía los campos con los nombres de Webpay y lee token y url")
    void crear() {
        servidor.expect(requestTo(BASE))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.buy_order").value("RES-2026-ABC123-1"))
                .andExpect(jsonPath("$.session_id").value("sesion-1"))
                .andExpect(jsonPath("$.amount").value(10000))
                .andExpect(jsonPath("$.return_url").value("http://localhost/retorno"))
                .andRespond(withSuccess("{\"token\":\"tok-1\",\"url\":\"https://webpay.test/form\"}", MediaType.APPLICATION_JSON));

        WebpayPlusClient.CrearRespuesta r = cliente.crear("RES-2026-ABC123-1", "sesion-1", 10000, "http://localhost/retorno");

        assertThat(r.token()).isEqualTo("tok-1");
        assertThat(r.url()).isEqualTo("https://webpay.test/form");
        servidor.verify();
    }

    @Test
    @DisplayName("confirmar lee una respuesta aprobada (response_code 0 y AUTHORIZED)")
    void confirmarAprobada() {
        servidor.expect(requestTo(BASE + "/tok-1"))
                .andExpect(method(HttpMethod.PUT))
                .andRespond(withSuccess("""
                        {"vci":"TSY","amount":10000,"status":"AUTHORIZED","buy_order":"RES-2026-ABC123-1",
                         "session_id":"sesion-1","card_detail":{"card_number":"6623"},
                         "accounting_date":"1120","transaction_date":"2026-11-20T15:00:00.000Z",
                         "authorization_code":"1213","payment_type_code":"VN","response_code":0,
                         "installments_number":0,"campo_nuevo_desconocido":"x"}
                        """, MediaType.APPLICATION_JSON));

        WebpayPlusClient.Resultado r = cliente.confirmar("tok-1");

        assertThat(r.aprobada()).isTrue();
        assertThat(r.amount()).isEqualByComparingTo(new BigDecimal("10000"));
        assertThat(r.cardDetail().cardNumber()).isEqualTo("6623");
        assertThat(r.authorizationCode()).isEqualTo("1213");
        assertThat(r.paymentTypeCode()).isEqualTo("VN");
        assertThat(r.buyOrder()).isEqualTo("RES-2026-ABC123-1");
    }

    @Test
    @DisplayName("una respuesta con response_code distinto de 0 o sin AUTHORIZED no es aprobada")
    void noAprobada() {
        assertThat(new WebpayPlusClient.Resultado(null, null, "FAILED", null, null, null, null, null, null, null, -1, 0).aprobada()).isFalse();
        assertThat(new WebpayPlusClient.Resultado(null, null, "AUTHORIZED", null, null, null, null, null, null, null, -1, 0).aprobada()).isFalse();
        assertThat(new WebpayPlusClient.Resultado(null, null, "FAILED", null, null, null, null, null, null, null, 0, 0).aprobada()).isFalse();
        assertThat(new WebpayPlusClient.Resultado(null, null, "AUTHORIZED", null, null, null, null, null, null, null, null, 0).aprobada()).isFalse();
    }

    @Test
    @DisplayName("consultar usa GET con el token en la ruta")
    void consultar() {
        servidor.expect(requestTo(BASE + "/tok-9"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("{\"status\":\"INITIALIZED\",\"amount\":500}", MediaType.APPLICATION_JSON));

        assertThat(cliente.consultar("tok-9").status()).isEqualTo("INITIALIZED");
    }

    @Test
    @DisplayName("un error HTTP de Webpay se informa con su código y sin filtrar el cuerpo")
    void errorHttp() {
        servidor.expect(requestTo(BASE + "/tok-2"))
                .andRespond(withStatus(HttpStatus.UNPROCESSABLE_ENTITY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"error_message\":\"detalle interno\"}"));

        assertThatThrownBy(() -> cliente.confirmar("tok-2"))
                .isInstanceOfSatisfying(WebpayException.class, e -> {
                    assertThat(e.getHttpStatus()).isEqualTo(422);
                    assertThat(e.getMessage()).doesNotContain("detalle interno");
                });
    }

    @Test
    @DisplayName("un corte de red se informa sin código HTTP")
    void errorDeRed() {
        servidor.expect(requestTo(BASE + "/tok-3"))
                .andRespond(request -> { throw new IOException("Read timed out"); });

        assertThatThrownBy(() -> cliente.confirmar("tok-3"))
                .isInstanceOfSatisfying(WebpayException.class, e -> assertThat(e.getHttpStatus()).isNull());
    }
}
