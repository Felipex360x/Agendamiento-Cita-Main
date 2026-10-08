package cl.nicolet.backend.repository;

import cl.nicolet.backend.model.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class CitaRepositoryTest {

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private TrabajadorRepository trabajadorRepository;

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("save y findByCodigoReserva - debe persistir y recuperar cita por codigo unico")
    void debeGuardarYRecuperarCitaPorCodigo() {
        Usuario u1 = usuarioRepository.save(new Usuario(null, "Ana", "Gomez", "ana@test.cl", "123"));
        Usuario u2 = usuarioRepository.save(new Usuario(null, "Pedro", "Soto", "pedro@test.cl", "123"));

        Cliente cliente = clienteRepository.save(new Cliente(null, u1, "+569111", "11111111-1", null, null, true));
        Trabajador trabajador = trabajadorRepository.save(new Trabajador(null, u2, "22222222-2", "+569222", "Estilista", null, BigDecimal.ZERO, true, null));
        Servicio servicio = servicioRepository.save(new Servicio(null, "Corte", "Desc", 45, new BigDecimal("15000"), true));

        LocalDateTime inicio = LocalDateTime.of(2026, 10, 20, 10, 0);
        LocalDateTime fin = inicio.plusMinutes(45);

        Cita cita = new Cita(
                null, "RES-2026-ABCD", cliente, trabajador, servicio,
                inicio, fin, EstadoCita.CONFIRMADA, new BigDecimal("15000"),
                "Notas", null, null
        );

        Cita guardada = citaRepository.save(cita);
        assertNotNull(guardada.getId());

        Optional<Cita> opt = citaRepository.findByCodigoReserva("RES-2026-ABCD");
        assertTrue(opt.isPresent());
        assertEquals("RES-2026-ABCD", opt.get().getCodigoReserva());
        assertEquals(EstadoCita.CONFIRMADA, opt.get().getEstado());
    }

    @Test
    @DisplayName("existeSolapamiento - debe detectar superposicion de citas del mismo trabajador")
    void debeDetectarSolapamientoDeCitas() {
        Usuario u1 = usuarioRepository.save(new Usuario(null, "Ana", "Gomez", "ana2@test.cl", "123"));
        Usuario u2 = usuarioRepository.save(new Usuario(null, "Pedro", "Soto", "pedro2@test.cl", "123"));

        Cliente cliente = clienteRepository.save(new Cliente(null, u1, "+569111", "11111111-2", null, null, true));
        Trabajador trabajador = trabajadorRepository.save(new Trabajador(null, u2, "22222222-3", "+569222", "Estilista", null, BigDecimal.ZERO, true, null));
        Servicio servicio = servicioRepository.save(new Servicio(null, "Peinado", "Desc", 60, new BigDecimal("20000"), true));

        LocalDateTime citaInicio = LocalDateTime.of(2026, 10, 20, 10, 0);
        LocalDateTime citaFin = LocalDateTime.of(2026, 10, 20, 11, 0);

        citaRepository.save(new Cita(
                null, "RES-EXISTENTE", cliente, trabajador, servicio,
                citaInicio, citaFin, EstadoCita.CONFIRMADA, new BigDecimal("20000"),
                null, null, null
        ));

        // Intento 1: se cruza con la cita existente (10:30 a 11:30)
        boolean solapa = citaRepository.existeSolapamiento(
                trabajador.getId(),
                LocalDateTime.of(2026, 10, 20, 10, 30),
                LocalDateTime.of(2026, 10, 20, 11, 30)
        );
        assertTrue(solapa, "Debe detectar que 10:30 se solapa con [10:00 - 11:00]");

        // Intento 2: horario libre después de la cita (11:00 a 12:00)
        boolean noSolapa = citaRepository.existeSolapamiento(
                trabajador.getId(),
                LocalDateTime.of(2026, 10, 20, 11, 0),
                LocalDateTime.of(2026, 10, 20, 12, 0)
        );
        assertFalse(noSolapa, "No debe solaparse con un horario posterior [11:00 - 12:00]");
    }
}
