package cl.nicolet.backend.repository;

import cl.nicolet.backend.model.HorarioDisponibilidad;
import cl.nicolet.backend.model.Trabajador;
import cl.nicolet.backend.model.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class HorarioDisponibilidadRepositoryTest {

    @Autowired
    private HorarioDisponibilidadRepository horarioDisponibilidadRepository;

    @Autowired
    private TrabajadorRepository trabajadorRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    @DisplayName("findByTrabajadorIdAndDiaSemanaAndActivoTrue - debe encontrar horario activo por dia de semana")
    void debeEncontrarHorarioPorTrabajadorYDia() {
        Usuario usuario = usuarioRepository.save(new Usuario(null, "Especialista", "Test", "pro@test.cl", "123"));
        Trabajador trabajador = trabajadorRepository.save(new Trabajador(null, usuario, "12345678-5", "+569111", "Estilista", null, BigDecimal.ZERO, true, null));

        HorarioDisponibilidad h = new HorarioDisponibilidad(
                null, trabajador, 1, // Lunes
                LocalTime.of(9, 0), LocalTime.of(18, 0),
                LocalTime.of(13, 0), LocalTime.of(14, 0),
                true
        );
        horarioDisponibilidadRepository.save(h);

        Optional<HorarioDisponibilidad> opt = horarioDisponibilidadRepository
                .findByTrabajadorIdAndDiaSemanaAndActivoTrue(trabajador.getId(), 1);

        assertTrue(opt.isPresent());
        assertEquals(LocalTime.of(9, 0), opt.get().getHoraInicio());
        assertEquals(LocalTime.of(18, 0), opt.get().getHoraFin());
    }

    @Test
    @DisplayName("findByTrabajadorIdAndActivoTrue - debe listar todos los turnos activos del trabajador")
    void debeListarTurnosActivosDelTrabajador() {
        Usuario usuario = usuarioRepository.save(new Usuario(null, "Especialista2", "Test2", "pro2@test.cl", "123"));
        Trabajador trabajador = trabajadorRepository.save(new Trabajador(null, usuario, "12345678-6", "+569222", "Colorista", null, BigDecimal.ZERO, true, null));

        horarioDisponibilidadRepository.save(new HorarioDisponibilidad(null, trabajador, 1, LocalTime.of(9, 0), LocalTime.of(18, 0), null, null, true));
        horarioDisponibilidadRepository.save(new HorarioDisponibilidad(null, trabajador, 2, LocalTime.of(9, 0), LocalTime.of(18, 0), null, null, true));
        horarioDisponibilidadRepository.save(new HorarioDisponibilidad(null, trabajador, 3, LocalTime.of(9, 0), LocalTime.of(18, 0), null, null, false)); // inactivo

        List<HorarioDisponibilidad> lista = horarioDisponibilidadRepository.findByTrabajadorIdAndActivoTrue(trabajador.getId());

        assertEquals(2, lista.size());
    }
}
