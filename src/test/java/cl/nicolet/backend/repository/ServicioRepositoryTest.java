package cl.nicolet.backend.repository;

import cl.nicolet.backend.model.Servicio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ServicioRepositoryTest {

    @Autowired
    private ServicioRepository servicioRepository;

    @Test
    @DisplayName("save - debe persistir el servicio y asignar ID")
    void debePersistirServicio() {
        Servicio s = new Servicio(null, "Esmaltado Semipermanente", "Descripción", 45, new BigDecimal("16000"), true);
        Servicio guardado = servicioRepository.save(s);

        assertNotNull(guardado.getId());
        assertEquals("Esmaltado Semipermanente", guardado.getNombre());
    }

    @Test
    @DisplayName("findByActivoTrue - debe retornar solo los servicios activos")
    void debeRetornarSoloActivos() {
        servicioRepository.save(new Servicio(null, "Servicio A", "Desc", 30, new BigDecimal("10000"), true));
        servicioRepository.save(new Servicio(null, "Servicio B", "Desc", 30, new BigDecimal("10000"), false));

        List<Servicio> activos = servicioRepository.findByActivoTrue();
        assertEquals(1, activos.size());
        assertEquals("Servicio A", activos.get(0).getNombre());
    }

    @Test
    @DisplayName("findByNombreIgnoreCase - debe encontrar el servicio sin importar mayusculas/minusculas")
    void debeEncontrarPorNombreIgnoreCase() {
        servicioRepository.save(new Servicio(null, "Peinado Gala", "Desc", 60, new BigDecimal("25000"), true));

        Optional<Servicio> opt = servicioRepository.findByNombreIgnoreCase("peinado GALA");
        assertTrue(opt.isPresent());
        assertEquals("Peinado Gala", opt.get().getNombre());
    }

    @Test
    @DisplayName("existsByNombreIgnoreCase - debe retornar true si ya existe el nombre")
    void debeRetornarTrueSiExisteNombre() {
        servicioRepository.save(new Servicio(null, "Manicura Rusa", "Desc", 60, new BigDecimal("22000"), true));

        assertTrue(servicioRepository.existsByNombreIgnoreCase("manicura rusa"));
        assertFalse(servicioRepository.existsByNombreIgnoreCase("otro servicio"));
    }
}
