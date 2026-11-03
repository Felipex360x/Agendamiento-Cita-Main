package cl.nicolet.backend.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Contrato único de errores de la API. Toda respuesta de error incluye:
 * <ul>
 *   <li>{@code status}: código HTTP;</li>
 *   <li>{@code codigo}: identificador estable (VALIDACION, CONFLICTO, NO_ENCONTRADO, SOLICITUD_INVALIDA,
 *       SERVICIO_NO_DISPONIBLE);</li>
 *   <li>{@code mensaje}: texto legible para mostrar al usuario;</li>
 *   <li>{@code error}: igual a {@code mensaje} (se conserva por compatibilidad con consumidores anteriores).</li>
 * </ul>
 * Las validaciones añaden {@code errores} ({campo: mensaje}) y, por compatibilidad, repiten cada campo al nivel
 * superior (comportamiento anterior; preferir {@code errores}).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static ResponseEntity<Map<String, Object>> respuesta(HttpStatus status, String codigo, String mensaje) {
        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("status", status.value());
        cuerpo.put("codigo", codigo);
        cuerpo.put("mensaje", mensaje);
        cuerpo.put("error", mensaje);
        return ResponseEntity.status(status).body(cuerpo);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(RecursoNoEncontradoException e) {
        return respuesta(HttpStatus.NOT_FOUND, "NO_ENCONTRADO", e.getMessage());
    }

    @ExceptionHandler(ServicioNoDisponibleException.class)
    public ResponseEntity<Map<String, Object>> handleServiceUnavailable(ServicioNoDisponibleException e) {
        return respuesta(HttpStatus.SERVICE_UNAVAILABLE, "SERVICIO_NO_DISPONIBLE", e.getMessage());
    }

    @ExceptionHandler(ConflictoException.class)
    public ResponseEntity<Map<String, Object>> handleConflicto(ConflictoException e) {
        return respuesta(HttpStatus.CONFLICT, "CONFLICTO", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        Map<String, String> errores = new LinkedHashMap<>();
        e.getBindingResult().getFieldErrors()
                .forEach(err -> errores.putIfAbsent(err.getField(), err.getDefaultMessage()));
        e.getBindingResult().getGlobalErrors()
                .forEach(err -> errores.putIfAbsent(err.getObjectName(), err.getDefaultMessage()));

        String resumen = errores.isEmpty()
                ? "Los datos enviados no son válidos."
                : String.join(" ", errores.values());
        ResponseEntity<Map<String, Object>> base = respuesta(HttpStatus.BAD_REQUEST, "VALIDACION", resumen);
        Map<String, Object> cuerpo = new LinkedHashMap<>(base.getBody());
        cuerpo.put("errores", errores);
        errores.forEach(cuerpo::putIfAbsent); // compatibilidad: campos al nivel superior
        return ResponseEntity.badRequest().body(cuerpo);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA", e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException e) {
        return respuesta(HttpStatus.CONFLICT, "CONFLICTO",
                "Operación rechazada por conflicto de integridad de datos o registro duplicado.");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleMessageNotReadable(HttpMessageNotReadableException e) {
        return respuesta(HttpStatus.BAD_REQUEST, "SOLICITUD_INVALIDA", "El formato de los datos enviados no es válido.");
    }
}
