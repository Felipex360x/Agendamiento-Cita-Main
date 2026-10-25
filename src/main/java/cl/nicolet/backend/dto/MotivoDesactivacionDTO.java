package cl.nicolet.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class MotivoDesactivacionDTO {

    @NotBlank(message = "Debes indicar el motivo de desactivación")
    @Size(max = 1000, message = "El motivo no puede superar los 1000 caracteres")
    private String motivo;

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
