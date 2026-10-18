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

import cl.nicolet.backend.model.Trabajador;
import cl.nicolet.backend.repository.TrabajadorRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ServicioService {

    private static final Logger log = LoggerFactory.getLogger(ServicioService.class);

    @Autowired
    private ServicioRepository servicioRepository;

    @Autowired(required = false)
    private TrabajadorRepository trabajadorRepository;

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
            Optional<Servicio> existenteOpt = servicioRepository.findByNombreIgnoreCase(dto.getNombre());
            if (existenteOpt.isPresent() && Boolean.FALSE.equals(existenteOpt.get().getActivo())) {
                Servicio inactivo = existenteOpt.get();
                inactivo.setNombre(dto.getNombre());
                inactivo.setDescripcion(dto.getDescripcion());
                inactivo.setDuracionMinutos(dto.getDuracionMinutos());
                inactivo.setPrecio(dto.getPrecio());
                inactivo.setActivo(true);
                Servicio reactivado = servicioRepository.save(inactivo);
                vincularServicioATrabajadoresActivos(reactivado);
                return toDTO(reactivado);
            }
            throw new IllegalArgumentException("Ya existe un servicio con el nombre: " + dto.getNombre());
        }
        Servicio s = new Servicio();
        s.setNombre(dto.getNombre());
        s.setDescripcion(dto.getDescripcion());
        s.setDuracionMinutos(dto.getDuracionMinutos());
        s.setPrecio(dto.getPrecio());
        s.setActivo(true);

        Servicio guardado = servicioRepository.save(s);
        vincularServicioATrabajadoresActivos(guardado);
        return toDTO(guardado);
    }

    private void vincularServicioATrabajadoresActivos(Servicio servicio) {
        if (trabajadorRepository == null || servicio == null) return;
        List<Trabajador> trabajadores = trabajadorRepository.findByActivoTrue();
        for (Trabajador t : trabajadores) {
            if (t.getServicios() != null && !t.getServicios().contains(servicio)) {
                t.getServicios().add(servicio);
                trabajadorRepository.save(t);
            }
        }
    }

    public ServicioDTO actualizar(Long id, ServicioCreateDTO dto) {
        log.info("Actualizando servicio id={}", id);
        Servicio s = servicioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio no encontrado: " + id));

        servicioRepository.findByNombreIgnoreCase(dto.getNombre())
                .filter(existente -> !existente.getId().equals(id))
                .ifPresent(existente -> {
                    throw new IllegalArgumentException("Ya existe otro servicio con el nombre: " + dto.getNombre());
                });

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
