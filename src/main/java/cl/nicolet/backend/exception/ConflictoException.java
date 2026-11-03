package cl.nicolet.backend.exception;

/** Conflicto con el estado actual de los datos (p. ej. correo ya registrado) → HTTP 409. */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
