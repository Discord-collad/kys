package principio.kys.repository;

import principio.kys.model.Curso;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CursoRepository extends JpaRepository<Curso, Integer> {
    List<Curso> findAllByOrderByNombreAsc();
}
