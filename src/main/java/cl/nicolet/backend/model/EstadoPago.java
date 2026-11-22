package cl.nicolet.backend.model;

/**
 * Situación del pago de una cita:
 * <pre>
 * PENDIENTE_PAGO -> PAGADO         (Webpay aprobó el pago)
 * PENDIENTE_PAGO -> (cita CANCELADA si el pago se rechaza, se anula o vence el plazo)
 * PAGO_EN_LOCAL                    (el cliente pagará en el estudio; no pasa por Webpay)
 * </pre>
 * Mientras una cita está en PENDIENTE_PAGO su horario queda bloqueado, pero todavía no se
 * le avisa al profesional ni se puede confirmar.
 */
public enum EstadoPago {
    PENDIENTE_PAGO,
    PAGADO,
    PAGO_EN_LOCAL
}
