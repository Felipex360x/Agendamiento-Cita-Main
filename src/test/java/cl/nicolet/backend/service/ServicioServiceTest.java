package cl.nicolet.backend.service;

import cl.nicolet.backend.dto.ServicioCreateDTO;
import cl.nicolet.backend.dto.ServicioDTO;
import cl.nicolet.backend.model.Servicio;
import cl.nicolet.backend.repository.ServicioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServicioServiceTest {

    @Mock
    private ServicioRepository servicioRepository;

    @InjectMocks
    private ServicioService servicioService;

    @Test
    @DisplayName("findAll - debe retornar lista de servicios activos")
    void debeRetornarServiciosActivos() {
        Servicio s1 = new Servicio(1L, "Corte", "Desc", 45, new BigDecimal("15000"), true);
        Servicio s2 = new Servicio(2L, "Manicura", "Desc", 60, new BigDecimal("20000"), true);
        when(servicioRepository.findByActivoTrue()).thenReturn(List.of(s1, s2));

        List<ServicioDTO> resultado = servicioService.findAll();

        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertEquals("Corte", resultado.get(0).getNombre());
    }

    @Test
    @DisplayName("crear - debe guardar y retornar el nuevo servicio")
    void debeCrearServicioExitosamente() {
        ServicioCreateDTO dto = new ServicioCreateDTO("Pedicura", "Desc", 45, new BigDecimal("18000"));
        Servicio guardado = new Servicio(10L, "Pedicura", "Desc", 45, new BigDecimal("18000"), true);

        when(servicioRepository.existsByNombreIgnoreCase("Pedicura")).thenReturn(false);
        when(servicioRepository.save(any(Servicio.class))).thenReturn(guardado);

        ServicioDTO resultado = servicioService.crear(dto);

        assertNotNull(resultado);
        assertEquals(10L, resultado.getId());
        assertEquals("Pedicura", resultado.getNombre());
        verify(servicioRepository, times(1)).save(any(Servicio.class));
    }

    @Test
    @DisplayName("crear - debe lanzar excepcion si el nombre ya existe")
    void debeLanzarExcepcionSiNombreYaExiste() {
        ServicioCreateDTO dto = new ServicioCreateDTO("Corte", "Desc", 45, new BigDecimal("15000"));
        when(servicioRepository.existsByNombreIgnoreCase("Corte")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> servicioService.crear(dto));
        verify(servicioRepository, never()).save(any(Servicio.class));
    }

    @Test
    @DisplayName("desactivar - debe cambiar activo a false")
    void debeDesactivarServicio() {
        Servicio s = new Servicio(1L, "Corte", "Desc", 45, new BigDecimal("15000"), true);
        when(servicioRepository.findById(1L)).thenReturn(Optional.of(s));

        servicioService.desactivar(1L);

        assertFalse(s.getActivo());
        verify(servicioRepository, times(1)).save(s);
    }
}
