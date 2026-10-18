package cl.nicolet.backend.dto;

import java.util.Objects;

public class AuthResponseDTO {

    private String token;
    private String tipoToken = "Bearer";
    private Long id;
    private String nombre;
    private String apellidoP;
    private String correo;
    private String rol;

    public AuthResponseDTO() {
    }

    public AuthResponseDTO(String token, String tipoToken, Long id, String nombre, String apellidoP, String correo, String rol) {
        this.token = token;
        this.tipoToken = tipoToken != null ? tipoToken : "Bearer";
        this.id = id;
        this.nombre = nombre;
        this.apellidoP = apellidoP;
        this.correo = correo;
        this.rol = rol;
    }

    public static AuthResponseDTOBuilder builder() {
        return new AuthResponseDTOBuilder();
    }

    public static class AuthResponseDTOBuilder {
        private String token;
        private String tipoToken = "Bearer";
        private Long id;
        private String nombre;
        private String apellidoP;
        private String correo;
        private String rol;

        public AuthResponseDTOBuilder token(String token) {
            this.token = token;
            return this;
        }

        public AuthResponseDTOBuilder tipoToken(String tipoToken) {
            this.tipoToken = tipoToken;
            return this;
        }

        public AuthResponseDTOBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public AuthResponseDTOBuilder nombre(String nombre) {
            this.nombre = nombre;
            return this;
        }

        public AuthResponseDTOBuilder apellidoP(String apellidoP) {
            this.apellidoP = apellidoP;
            return this;
        }

        public AuthResponseDTOBuilder correo(String correo) {
            this.correo = correo;
            return this;
        }

        public AuthResponseDTOBuilder rol(String rol) {
            this.rol = rol;
            return this;
        }

        public AuthResponseDTO build() {
            return new AuthResponseDTO(token, tipoToken, id, nombre, apellidoP, correo, rol);
        }
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTipoToken() {
        return tipoToken;
    }

    public void setTipoToken(String tipoToken) {
        this.tipoToken = tipoToken;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getApellidoP() {
        return apellidoP;
    }

    public void setApellidoP(String apellidoP) {
        this.apellidoP = apellidoP;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthResponseDTO that)) return false;
        return Objects.equals(token, that.token)
                && Objects.equals(tipoToken, that.tipoToken)
                && Objects.equals(id, that.id)
                && Objects.equals(nombre, that.nombre)
                && Objects.equals(apellidoP, that.apellidoP)
                && Objects.equals(correo, that.correo)
                && Objects.equals(rol, that.rol);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token, tipoToken, id, nombre, apellidoP, correo, rol);
    }
}
