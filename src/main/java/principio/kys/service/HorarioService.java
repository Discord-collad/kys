package principio.kys.service;

import principio.kys.model.Horario;
import principio.kys.model.HorarioUsuario;
import principio.kys.repository.HorarioRepository;
import principio.kys.repository.HorarioUsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class HorarioService {
    private final HorarioRepository horarioRepo;
    private final HorarioUsuarioRepository horarioUsuarioRepo;

    @Autowired
    public HorarioService(HorarioRepository horarioRepo, HorarioUsuarioRepository horarioUsuarioRepo) {
        this.horarioRepo = horarioRepo;
        this.horarioUsuarioRepo = horarioUsuarioRepo;
    }

    public List<Horario> listarTodos() { return horarioRepo.findAllByOrderByNombreAsc(); }

    public Optional<Horario> buscarPorId(@NonNull Integer id) { return horarioRepo.findById(id); }

    public Horario obtenerPorId(@NonNull Integer id) {
        return horarioRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Horario no encontrado: " + id));
    }

    public Horario guardar(@NonNull Horario h) { return horarioRepo.save(h); }

    @Transactional
    public void eliminar(@NonNull Integer id) {
        horarioRepo.deleteById(id);
    }

    @SuppressWarnings("null")
    public Optional<Horario> obtenerHorarioDeUsuario(Integer usuarioId) {
        return horarioUsuarioRepo.findFirstByUsuarioId(usuarioId)
                .flatMap(hu -> horarioRepo.findById(hu.getHorarioId()));
    }

    @Transactional
    public void asignarHorarioAUsuario(Integer usuarioId, Integer horarioId) {
        horarioUsuarioRepo.deleteByUsuarioId(usuarioId);
        if (horarioId != null) {
            HorarioUsuario hu = new HorarioUsuario();
            hu.setUsuarioId(usuarioId);
            hu.setHorarioId(horarioId);
            horarioUsuarioRepo.save(hu);
        }
    }
}
