package cl.nicolet.backend.payment;

/** Error al hablar con Webpay (sin conexión, respuesta inválida o rechazo de la API). */
public class WebpayException extends RuntimeException {

    private final Integer httpStatus;

    public WebpayException(String message, Integer httpStatus, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    /** Código HTTP de la respuesta, o null si ni siquiera hubo respuesta (error de red o timeout). */
    public Integer getHttpStatus() {
        return httpStatus;
    }
}
