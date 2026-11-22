package cl.nicolet.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Intento de pago de una cita con Webpay Plus. Una cita puede tener varios intentos
 * (cada uno con su propia orden de compra), pero solo uno puede quedar APROBADO.
 */
@Entity
@Table(name = "pago")
public class Pago {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cita_id", nullable = false)
    private Cita cita;

    /** Orden de compra enviada a Webpay (máx. 26 caracteres, única por transacción). */
    @Column(name = "buy_order", nullable = false, unique = true, length = 26)
    private String buyOrder;

    /**
     * Valor aleatorio enviado a Webpay como session_id. Webpay lo devuelve cuando el cliente
     * anula o se vence el formulario, y sirve para comprobar que ese aviso es legítimo.
     */
    @Column(name = "session_id", nullable = false, length = 61)
    private String sessionId;

    @Column(length = 64, unique = true)
    private String token;

    @Column(name = "url_pago", length = 255)
    private String urlPago;

    /** Monto en pesos chilenos, sin decimales. */
    @Column(nullable = false)
    private Long monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private EstadoTransaccionPago estado = EstadoTransaccionPago.INICIADO;

    @Column(name = "response_code")
    private Integer responseCode;

    @Column(name = "authorization_code", length = 10)
    private String authorizationCode;

    @Column(name = "payment_type_code", length = 5)
    private String paymentTypeCode;

    @Column(name = "installments_number")
    private Integer installmentsNumber;

    @Column(name = "card_last4", length = 4)
    private String cardLast4;

    @Column(name = "transaction_date", length = 30)
    private String transactionDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "updated_at")
    private LocalDateTime actualizadoEn;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Cita getCita() { return cita; }
    public void setCita(Cita cita) { this.cita = cita; }
    public String getBuyOrder() { return buyOrder; }
    public void setBuyOrder(String buyOrder) { this.buyOrder = buyOrder; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getUrlPago() { return urlPago; }
    public void setUrlPago(String urlPago) { this.urlPago = urlPago; }
    public Long getMonto() { return monto; }
    public void setMonto(Long monto) { this.monto = monto; }
    public EstadoTransaccionPago getEstado() { return estado; }
    public void setEstado(EstadoTransaccionPago estado) { this.estado = estado; }
    public Integer getResponseCode() { return responseCode; }
    public void setResponseCode(Integer responseCode) { this.responseCode = responseCode; }
    public String getAuthorizationCode() { return authorizationCode; }
    public void setAuthorizationCode(String authorizationCode) { this.authorizationCode = authorizationCode; }
    public String getPaymentTypeCode() { return paymentTypeCode; }
    public void setPaymentTypeCode(String paymentTypeCode) { this.paymentTypeCode = paymentTypeCode; }
    public Integer getInstallmentsNumber() { return installmentsNumber; }
    public void setInstallmentsNumber(Integer installmentsNumber) { this.installmentsNumber = installmentsNumber; }
    public String getCardLast4() { return cardLast4; }
    public void setCardLast4(String cardLast4) { this.cardLast4 = cardLast4; }
    public String getTransactionDate() { return transactionDate; }
    public void setTransactionDate(String transactionDate) { this.transactionDate = transactionDate; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public void setCreadoEn(LocalDateTime creadoEn) { this.creadoEn = creadoEn; }
    public LocalDateTime getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(LocalDateTime actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}
