package cl.nicolet.backend.service;

import cl.nicolet.backend.dto.ServicioCreateDTO;
import cl.nicolet.backend.dto.ServicioDTO;
import cl.nicolet.backend.exception.RecursoNoEncontradoException;
import cl.nicolet.backend.model.Servicio;
import cl.nicolet.backend.repository.ServicioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ServicioService {

    private static final Logger log = LoggerFactory.getLogger(ServicioService.class);

    @Autowired
    private ServicioRepository servicioRepository;

    public List<ServicioDTO> findAll() {
        log.info("Consultando todos los servicios activos");
        return servicioRepository.findByActivoTrue().stream().map(this::toDTO).collect(Collectors.toList());
    }

    public ServicioDTO findById(Long id) {
        log.info("Buscando servicio con id={}", id);
        Servicio s = servicioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado: " + id));
        return toDTO(s);
    }

    public ServicioDTO crear(ServicioCreateDTO dto) {
        log.info("Creando nuevo servicio nombre={}", dto.getNombre());
        if (servicioRepository.existsByNombreIgnoreCase(dto.getNombre())) {
            throw new IllegalArgumentException("Ya existe un servicio con el nombre: " + dto.getNombre());
        }
        Servicio s = new Servicio();
        s.setNombre(dto.getNombre());
        s.setDescripcion(dto.getDescripcion());
        s.setDuracionMinutos(dto.getDuracionMinutos());
        s.setPrecio(dto.getPrecio());
        s.setActivo(true);

        return toDTO(servicioRepository.save(s));
    }

    public ServicioDTO actualizar(Long id, ServicioCreateDTO dto) {
        log.info("Actualizando servicio id={}", id);
        Servicio s = servicioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado: " + id));

        s.setNombre(dto.getNombre());
        s.setDescripcion(dto.getDescripcion());
        s.setDuracionMinutos(dto.getDuracionMinutos());
        s.setPrecio(dto.getPrecio());

        return toDTO(servicioRepository.save(s));
    }

    public void desactivar(Long id) {
        log.info("Desactivando servicio id={}", id);
        Servicio s = servicioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado: " + id));
        s.setActivo(false);
        servicioRepository.save(s);
    }

    public ServicioDTO toDTO(Servicio s) {
        return new ServicioDTO(
                s.getId(),
                s.getNombre(),
                s.getDescripcion(),
                s.getDuracionMinutos(),
                s.getPrecio(),
                s.getActivo()
        );
    }
}
