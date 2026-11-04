package cl.nicolet.backend.dto;

public class CorreoEnviadoDTO {

    private Long id;
    private String tipo;
    private String destinatario;
    private String nombreDestinatario;
    private String asunto;
    private String codigoReserva;
    private String estadoEnvio;
    private String detalleEnvio;
    private String fechaHora;
    private String html;

    public CorreoEnviadoDTO() {
    }

    public CorreoEnviadoDTO(Long id, String tipo, String destinatario, String nombreDestinatario,
                            String asunto, String codigoReserva, String estadoEnvio,
                            String detalleEnvio, String fechaHora, String html) {
        this.id = id;
        this.tipo = tipo;
        this.destinatario = destinatario;
        this.nombreDestinatario = nombreDestinatario;
        this.asunto = asunto;
        this.codigoReserva = codigoReserva;
        this.estadoEnvio = estadoEnvio;
        this.detalleEnvio = detalleEnvio;
        this.fechaHora = fechaHora;
        this.html = html;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(String destinatario) {
        this.destinatario = destinatario;
    }

    public String getNombreDestinatario() {
        return nombreDestinatario;
    }

    public void setNombreDestinatario(String nombreDestinatario) {
        this.nombreDestinatario = nombreDestinatario;
    }

    public String getAsunto() {
        return asunto;
    }

    public void setAsunto(String asunto) {
        this.asunto = asunto;
    }

    public String getCodigoReserva() {
        return codigoReserva;
    }

    public void setCodigoReserva(String codigoReserva) {
        this.codigoReserva = codigoReserva;
    }

    public String getEstadoEnvio() {
        return estadoEnvio;
    }

    public void setEstadoEnvio(String estadoEnvio) {
        this.estadoEnvio = estadoEnvio;
    }

    public String getDetalleEnvio() {
        return detalleEnvio;
    }

    public void setDetalleEnvio(String detalleEnvio) {
        this.detalleEnvio = detalleEnvio;
    }

    public String getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(String fechaHora) {
        this.fechaHora = fechaHora;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }
}
