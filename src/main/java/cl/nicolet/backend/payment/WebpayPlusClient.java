package cl.nicolet.backend.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;

/**
 * Cliente de la API REST de Webpay Plus (v1.2) de Transbank.
 * Solo traduce llamadas HTTP; las reglas de negocio están en PagoService.
 */
@Component
public class WebpayPlusClient {

    static final String RUTA_BASE = "/rswebpaytransaction/api/webpay/v1.2/transactions";
    static final String HOST_INTEGRACION = "https://webpay3gint.transbank.cl";
    static final String HOST_PRODUCCION = "https://webpay3g.transbank.cl";

    private final RestClient restClient;

    @Autowired
    public WebpayPlusClient(
            @Value("${webpay.environment:INTEGRATION}") String ambiente,
            @Value("${webpay.commerce-code}") String codigoComercio,
            @Value("${webpay.api-secret}") String llaveSecreta,
            @Value("${webpay.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${webpay.read-timeout-ms:15000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(connectTimeoutMs);
        fabrica.setReadTimeout(readTimeoutMs);
        String host = "PRODUCTION".equalsIgnoreCase(ambiente) ? HOST_PRODUCCION : HOST_INTEGRACION;
        this.restClient = RestClient.builder()
                .baseUrl(host + RUTA_BASE)
                .requestFactory(fabrica)
                .defaultHeader("Tbk-Api-Key-Id", codigoComercio)
                .defaultHeader("Tbk-Api-Key-Secret", llaveSecreta)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /** Constructor para pruebas: recibe un RestClient ya armado (por ejemplo, con un servidor simulado). */
    WebpayPlusClient(RestClient restClient) {
        this.restClient = restClient;
    }

    /** Crea una transacción. El navegador debe ir después a {@code url} enviando {@code token_ws}. */
    public CrearRespuesta crear(String buyOrder, String sessionId, long monto, String returnUrl) {
        return ejecutar("crear la transacción", () -> restClient.post()
                .body(new CrearSolicitud(buyOrder, sessionId, monto, returnUrl))
                .retrieve()
                .body(CrearRespuesta.class));
    }

    /** Confirma (captura) la transacción. Solo debe llamarse una vez, cuando el cliente vuelve del formulario. */
    public Resultado confirmar(String token) {
        return ejecutar("confirmar la transacción", () -> restClient.put()
                .uri("/{token}", token)
                .retrieve()
                .body(Resultado.class));
    }

    /** Consulta el estado de una transacción (sirve cuando no se supo el resultado de confirmar). */
    public Resultado consultar(String token) {
        return ejecutar("consultar la transacción", () -> restClient.get()
                .uri("/{token}", token)
                .retrieve()
                .body(Resultado.class));
    }

    private <T> T ejecutar(String accion, java.util.function.Supplier<T> llamada) {
        try {
            T respuesta = llamada.get();
            if (respuesta == null) {
                throw new WebpayException("Webpay respondió vacío al " + accion, null, null);
            }
            return respuesta;
        } catch (RestClientResponseException e) {
            // No se incluye el cuerpo completo: puede traer datos de la transacción.
            throw new WebpayException("Webpay rechazó la solicitud al " + accion + " (HTTP "
                    + e.getStatusCode().value() + ")", e.getStatusCode().value(), e);
        } catch (RestClientException e) {
            throw new WebpayException("No se pudo comunicar con Webpay al " + accion, null, e);
        }
    }

    record CrearSolicitud(
            @JsonProperty("buy_order") String buyOrder,
            @JsonProperty("session_id") String sessionId,
            long amount,
            @JsonProperty("return_url") String returnUrl) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CrearRespuesta(String token, String url) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Resultado(
            String vci,
            BigDecimal amount,
            String status,
            @JsonProperty("buy_order") String buyOrder,
            @JsonProperty("session_id") String sessionId,
            @JsonProperty("card_detail") DetalleTarjeta cardDetail,
            @JsonProperty("accounting_date") String accountingDate,
            @JsonProperty("transaction_date") String transactionDate,
            @JsonProperty("authorization_code") String authorizationCode,
            @JsonProperty("payment_type_code") String paymentTypeCode,
            @JsonProperty("response_code") Integer responseCode,
            @JsonProperty("installments_number") Integer installmentsNumber) {

        /** Aprobada solo si el código de respuesta es 0 y el estado es AUTHORIZED. */
        public boolean aprobada() {
            return responseCode != null && responseCode == 0 && "AUTHORIZED".equals(status);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record DetalleTarjeta(@JsonProperty("card_number") String cardNumber) {
    }
}
