package principio.kys.service;

import principio.kys.model.PeriodoLectivo;
import principio.kys.repository.PeriodoLectivoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class PeriodoLectivoService {
    private final PeriodoLectivoRepository periodoRepo;

    public PeriodoLectivoService(PeriodoLectivoRepository periodoRepo) {
        this.periodoRepo = periodoRepo;
    }

    public List<PeriodoLectivo> listarTodos() {
        return periodoRepo.findAll();
    }

    @SuppressWarnings("null")
    public Optional<PeriodoLectivo> buscarPorId(Integer id) {
        return periodoRepo.findById(id);
    }

    public Optional<PeriodoLectivo> obtenerPeriodoActivo() {
        return periodoRepo.findByActivoTrue();
    }

    public Optional<PeriodoLectivo> obtenerUltimoPeriodo() {
        return periodoRepo.findByEsUltimoPeriodoTrue();
    }

    /**
     * Guarda un periodo validando las reglas del proyecto:
     * nombre obligatorio, fecha fin posterior a fecha inicio, duracion >= 1 semana.
     * Un periodo nuevo nunca entra activo por defecto (se activa explicitamente).
     */
    @Transactional
    public PeriodoLectivo guardar(PeriodoLectivo p) {
        validar(p);
        if (p.getId() == null) {
            p.setActivo(false);
            p.setEsUltimoPeriodo(false);
        }
        return periodoRepo.save(p);
    }

    private void validar(PeriodoLectivo p) {
        if (p == null) {
            throw new IllegalArgumentException("El periodo no puede ser nulo");
        }
        if (p.getNombre() == null || p.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre del periodo es obligatorio");
        }
        if (p.getNombre().length() > 50) {
            throw new IllegalArgumentException("El nombre del periodo no puede superar 50 caracteres");
        }
        if (p.getFechaInicio() == null || p.getFechaFin() == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin son obligatorias");
        }
        if (!p.getFechaFin().isAfter(p.getFechaInicio())) {
            throw new IllegalArgumentException("La fecha fin debe ser posterior a la fecha inicio");
        }
        if (p.getDuracionSemanas() == null || p.getDuracionSemanas() < 1) {
            throw new IllegalArgumentException("La duración debe ser de al menos 1 semana");
        }
    }

    @Transactional
    @SuppressWarnings("null")
    public void eliminar(Integer id) {
        if (periodoRepo.findById(id).isEmpty()) {
            throw new IllegalArgumentException("Periodo no encontrado: " + id);
        }
        periodoRepo.deleteById(id);
    }

    /** Activa un periodo y desactiva todos los demás (solo uno activo a la vez). */
    @Transactional
    @SuppressWarnings("null")
    public void activarPeriodo(Integer id) {
        PeriodoLectivo periodo = periodoRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Periodo no encontrado: " + id));
        periodoRepo.desactivarTodos();
        periodo.setActivo(true);
        periodoRepo.save(periodo);
    }

    /** Marca un periodo como "último" y desmarca todos los demás. */
    @Transactional
    @SuppressWarnings("null")
    public void marcarComoUltimoPeriodo(Integer id) {
        PeriodoLectivo periodo = periodoRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Periodo no encontrado: " + id));
        periodoRepo.desmarcarUltimoDeTodos();
        periodo.setEsUltimoPeriodo(true);
        periodoRepo.save(periodo);
    }

    /** Indica si hoy cae dentro de las fechas del periodo activo (útil para avisos). */
    @Transactional(readOnly = true)
    public boolean hoyEstaDentroDelPeriodoActivo() {
        return obtenerPeriodoActivo()
                .filter(p -> {
                    LocalDate hoy = LocalDate.now();
                    return !hoy.isBefore(p.getFechaInicio()) && !hoy.isAfter(p.getFechaFin());
                })
                .isPresent();
    }
}
