package cl.nicolet.backend.model;

/** Resultado de un intento de pago con Webpay Plus. */
public enum EstadoTransaccionPago {
    /** Se creó la transacción en Webpay y se espera que el cliente pague. */
    INICIADO,
    /** Webpay autorizó el pago (response_code 0 y status AUTHORIZED). */
    APROBADO,
    /** Webpay rechazó el pago. */
    RECHAZADO,
    /** El cliente anuló el pago en el formulario de Webpay. */
    ANULADO_POR_USUARIO,
    /** Venció el plazo para pagar (el horario se liberó). */
    EXPIRADO,
    /** No se pudo confirmar el pago con Webpay. */
    ERROR
}
