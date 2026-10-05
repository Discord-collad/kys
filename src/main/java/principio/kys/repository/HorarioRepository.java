package principio.kys.repository;

import principio.kys.model.Horario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HorarioRepository extends JpaRepository<Horario, Integer> {
    List<Horario> findAllByOrderByNombreAsc();
}
