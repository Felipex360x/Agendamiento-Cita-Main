package cl.nicolet.backend.exception;

/** Error cuando un dato choca con otro ya guardado (por ejemplo, correo repetido) -> HTTP 409. */
public class ConflictoException extends RuntimeException {
    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
