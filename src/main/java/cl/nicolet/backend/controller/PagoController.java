package cl.nicolet.backend.controller;

import cl.nicolet.backend.dto.PagoHistorialRespuestaDTO;
import cl.nicolet.backend.dto.PagoInicioDTO;
import cl.nicolet.backend.model.EstadoTransaccionPago;
import cl.nicolet.backend.service.PagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.Locale;

@Tag(name = "Pagos", description = "Pago de citas con Webpay Plus")
@RestController
@RequestMapping("/api/v2/nicolet/pagos/webpay")
public class PagoController {

    @Autowired
    private PagoService pagoService;

    @Value("${app.frontend.base-url:http://localhost:5173}")
    private String frontendBaseUrl;

    @Operation(summary = "Iniciar el pago de una cita en Webpay",
            description = "Devuelve el token y la URL de Webpay. El navegador debe enviar por POST el campo token_ws a esa URL.")
    @PostMapping("/citas/{citaId}/iniciar")
    @PreAuthorize("hasRole('ADMINISTRADOR') or @citaService.esClientePropioDeCita(#p0, principal.id)")
    public ResponseEntity<PagoInicioDTO> iniciar(@Parameter(description = "ID de la cita") @PathVariable Long citaId) {
        return ResponseEntity.ok(pagoService.iniciarPago(citaId));
    }

    @Operation(summary = "Historial de pagos (administración)",
            description = "Pagos del más nuevo al más antiguo. Filtros opcionales: estado y rango de fechas (yyyy-MM-dd) del día en que se inició el pago.")
    @GetMapping("/historial")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public ResponseEntity<PagoHistorialRespuestaDTO> historial(
            @RequestParam(name = "estado", required = false) EstadoTransaccionPago estado,
            @RequestParam(name = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(name = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(pagoService.historial(estado, desde, hasta));
    }

    @Operation(summary = "Regreso desde Webpay (público)",
            description = "Webpay devuelve aquí al cliente al terminar, anular o vencer el formulario de pago. "
                    + "Confirma el pago y redirige a la página de resultado del sitio.")
    @RequestMapping(value = "/retorno", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Void> retorno(
            @RequestParam(name = "token_ws", required = false) String tokenWs,
            @RequestParam(name = "TBK_TOKEN", required = false) String tbkToken,
            @RequestParam(name = "TBK_ORDEN_COMPRA", required = false) String ordenCompra,
            @RequestParam(name = "TBK_ID_SESION", required = false) String idSesion) {
        PagoService.RetornoPago retorno;
        try {
            retorno = pagoService.procesarRetorno(tokenWs, tbkToken, ordenCompra, idSesion);
        } catch (RuntimeException e) {
            retorno = new PagoService.RetornoPago(PagoService.ResultadoRetorno.ERROR, null);
        }
        UriComponentsBuilder destino = UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .path("/pago-resultado.html")
                .queryParam("estado", retorno.resultado().name().toLowerCase(Locale.ROOT));
        if (retorno.codigoReserva() != null) {
            destino.queryParam("codigo", retorno.codigoReserva());
        }
        URI uri = destino.build().encode().toUri();
        return ResponseEntity.status(HttpStatus.SEE_OTHER).location(uri).build();
    }
}
