package cl.nicolet.backend.dto;

/** Forma de pago elegida al reservar. */
public enum ModalidadPago {
    /** Pago con Webpay Plus; el horario queda bloqueado hasta que el pago se complete. */
    ONLINE,
    /** El cliente paga en el estudio. */
    LOCAL
}
