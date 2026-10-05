package principio.kys.service;

import principio.kys.model.Curso;
import principio.kys.model.CursoUsuario;
import principio.kys.repository.CursoRepository;
import principio.kys.repository.CursoUsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class CursoService {
    private final CursoRepository cursoRepo;
    private final CursoUsuarioRepository cursoUsuarioRepo;

    @Autowired
    public CursoService(CursoRepository cursoRepo, CursoUsuarioRepository cursoUsuarioRepo) {
        this.cursoRepo = cursoRepo;
        this.cursoUsuarioRepo = cursoUsuarioRepo;
    }

    public List<Curso> listarTodos() { return cursoRepo.findAllByOrderByNombreAsc(); }

    @SuppressWarnings("null")
    public Optional<Curso> buscarPorId(Integer id) { return cursoRepo.findById(id); }

    @SuppressWarnings("null")
    public Curso obtenerPorId(Integer id) {
        return cursoRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Curso no encontrado: " + id));
    }

    @SuppressWarnings("null")
    public Curso guardar(Curso c) { return cursoRepo.save(c); }

    @Transactional
    @SuppressWarnings("null")
    public void eliminar(Integer id) { cursoRepo.deleteById(id); }

    @SuppressWarnings("null")
    public List<Curso> cursosDeUsuario(Integer usuarioId) {
        return cursoUsuarioRepo.findByUsuarioId(usuarioId).stream()
                .map(cu -> cursoRepo.findById(cu.getCursoId()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    @Transactional
    public void asignarCursosAUsuario(Integer usuarioId, List<Integer> cursoIds) {
        cursoUsuarioRepo.deleteByUsuarioId(usuarioId);
        if (cursoIds != null) {
            for (Integer cursoId : cursoIds) {
                if (cursoId == null) continue;
                CursoUsuario cu = new CursoUsuario();
                cu.setUsuarioId(usuarioId);
                cu.setCursoId(cursoId);
                cursoUsuarioRepo.save(cu);
            }
        }
    }

    @Transactional
    public void asignarUsuariosACurso(Integer cursoId, List<Integer> usuarioIds) {
        cursoUsuarioRepo.findByCursoId(cursoId).forEach(cursoUsuarioRepo::delete);
        if (usuarioIds != null) {
            for (Integer usuarioId : usuarioIds) {
                if (usuarioId == null) continue;
                CursoUsuario cu = new CursoUsuario();
                cu.setCursoId(cursoId);
                cu.setUsuarioId(usuarioId);
                cursoUsuarioRepo.save(cu);
            }
        }
    }
}
