package principio.kys.service;

import principio.kys.model.Asistencia;
import principio.kys.model.Horario;
import principio.kys.model.Usuario;
import principio.kys.repository.AsistenciaRepository;
import principio.kys.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
public class AsistenciaService {
    private final AsistenciaRepository asistenciaRepo;
    private final HorarioService horarioService;
    private final UsuarioRepository usuarioRepo;

    @Autowired
    public AsistenciaService(AsistenciaRepository asistenciaRepo, HorarioService horarioService,
                             UsuarioRepository usuarioRepo) {
        this.asistenciaRepo = asistenciaRepo;
        this.horarioService = horarioService;
        this.usuarioRepo = usuarioRepo;
    }

    public Optional<Asistencia> obtenerDeHoy(Integer usuarioId) {
        return asistenciaRepo.findFirstByUsuarioIdAndFechaOrderByIdDesc(usuarioId, LocalDate.now());
    }

    @SuppressWarnings("null")
    public Optional<Usuario> buscarUsuarioParaEntrada(Integer usuarioId) {
        return usuarioRepo.findById(usuarioId);
    }

    public List<Asistencia> listarPorUsuario(Integer usuarioId) {
        return asistenciaRepo.findByUsuarioId(usuarioId);
    }

    public List<Asistencia> listarPorFechas(LocalDate inicio, LocalDate fin) {
        return asistenciaRepo.findByFechaBetween(inicio, fin);
    }

    public List<Asistencia> listarPorEstado(String estado) {
        return asistenciaRepo.findByEstadoAsistenciaOrderByFechaDesc(estado);
    }

    public List<Asistencia> listarDeHoy() {
        return asistenciaRepo.findByFechaOrderByUsuarioAsc(LocalDate.now());
    }

    public long contarAsistenciasHoy() {
        return asistenciaRepo.countByFecha(LocalDate.now());
    }

    public long contarAprobadasHoy() {
        return asistenciaRepo.countByFechaAndEstadoAsistencia(LocalDate.now(), "APROBADO");
    }

    // [P3] Conteos en BD (reemplazan listarPorEstado(...).size() y listarDeHoy().stream())
    public long contarPorEstado(String estado) {
        return asistenciaRepo.countByEstadoAsistencia(estado);
    }

    public long contarHoyPorEstado(String estado) {
        return asistenciaRepo.countByFechaAndEstadoAsistencia(LocalDate.now(), estado);
    }

    /** Guarda cambios directos sobre una asistencia existente (usado por tests y flujos internos). */
    @Transactional
    public Asistencia guardar(@NonNull Asistencia a) {
        return asistenciaRepo.save(a);
    }

    @Transactional
    public Asistencia registrarEntrada(Usuario usuario) {
        LocalTime ahora = LocalTime.now();
        Optional<Horario> horarioOpt = horarioService.obtenerHorarioDeUsuario(usuario.getId());
        String estado = "PENDIENTE";
        String obs = horarioOpt.map(h -> {
            LocalTime limiteTarde = h.getHoraEntrada().plusMinutes(h.getToleranciaMinutos());
            return ahora.isAfter(limiteTarde) ? "Entrada tarde (límite " + limiteTarde + ")" : null;
        }).orElse("Sin horario asignado");

        Asistencia a = new Asistencia();
        a.setUsuario(usuario);
        a.setFecha(LocalDate.now());
        a.setHoraEntrada(ahora);
        a.setEstadoAsistencia(estado);
        a.setObservaciones(obs);
        return asistenciaRepo.save(a);
    }

    /** [P1] Entrada de dirección desde el panel admin (idempotente: reutiliza la de hoy si existe). */
    @Transactional
    public Optional<Asistencia> registrarDirecta(@NonNull Integer usuarioId) {
        if (asistenciaRepo.existsByUsuarioIdAndFecha(usuarioId, LocalDate.now())) {
            return obtenerDeHoy(usuarioId);
        }
        return usuarioRepo.findById(usuarioId)
                .map(usuario -> {
                    Asistencia a = new Asistencia();
                    a.setUsuario(usuario);
                    a.setFecha(LocalDate.now());
                    a.setHoraEntrada(LocalTime.now());
                    a.setEstadoAsistencia("APROBADO");
                    return asistenciaRepo.save(a);
                });
    }

    /**
     * [P1] Salida del alumno: NO crea asistencia fantasma si no hay entrada.
     * Si la salida queda antes o igual que la entrada, lanza excepción (el controller la traduce en mensaje).
     */
    @Transactional
    public Optional<Asistencia> registrarSalida(Usuario usuario) {
        return obtenerDeHoy(usuario.getId()).map(a -> {
            if (a.getHoraSalida() == null) {
                LocalTime ahora = LocalTime.now();
                if (a.getHoraEntrada() != null && !ahora.isAfter(a.getHoraEntrada())) {
                    throw new IllegalArgumentException("La hora de salida debe ser posterior a la entrada");
                }
                a.setHoraSalida(ahora);
                return asistenciaRepo.save(a);
            }
            return a;
        });
    }

    @Transactional
    public Asistencia validar(Integer asistenciaId, Usuario profesor, boolean aprobado, String observacion) {
        if (asistenciaId == null) {
            throw new IllegalArgumentException("ID de asistencia no puede ser null");
        }
        Asistencia a = asistenciaRepo.findById(asistenciaId)
                .orElseThrow(() -> new IllegalArgumentException("Asistencia no encontrada: " + asistenciaId));
        a.setEstadoAsistencia(aprobado ? "APROBADO" : "RECHAZADO");
        a.setValidadoPor(profesor);
        a.setHoraValidacion(LocalTime.now());
        if (observacion != null && !observacion.isBlank()) {
            a.setObservaciones(observacion);
        }
        return asistenciaRepo.save(a);
    }

    @Transactional
    @SuppressWarnings("null")
    public Asistencia registrarDirecta(@NonNull Usuario usuario) {
        return registrarDirecta(usuario.getId()).orElseThrow(() ->
                new IllegalArgumentException("Usuario no encontrado: " + usuario.getId()));
    }

    @Transactional
    @SuppressWarnings("null")
    public Optional<Asistencia> registrarSalidaDirecta(Integer asistenciaId) {
        return asistenciaRepo.findById(asistenciaId).map(a -> {
            if (a.getHoraSalida() == null) {
                LocalTime ahora = LocalTime.now();
                if (a.getHoraEntrada() != null && !ahora.isAfter(a.getHoraEntrada())) {
                    throw new IllegalArgumentException("La hora de salida debe ser posterior a la entrada");
                }
                a.setHoraSalida(ahora);
                return asistenciaRepo.save(a);
            }
            return a;
        });
    }
}
