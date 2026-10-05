package principio.kys.repository;

import principio.kys.model.Asistencia;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AsistenciaRepository extends JpaRepository<Asistencia, Integer> {
    List<Asistencia> findByUsuarioId(Integer usuarioId);
    List<Asistencia> findByFechaBetween(LocalDate inicio, LocalDate fin);
    Optional<Asistencia> findFirstByUsuarioIdAndFechaOrderByIdDesc(Integer usuarioId, LocalDate fecha);
    List<Asistencia> findByEstadoAsistenciaOrderByFechaDesc(String estadoAsistencia);
    List<Asistencia> findByFechaOrderByUsuarioAsc(LocalDate fecha);
    long countByFecha(LocalDate fecha);
    long countByFechaAndEstadoAsistencia(LocalDate fecha, String estadoAsistencia);
    void deleteByUsuarioId(Integer usuarioId);

    // [P1] Guardas de integridad y [P3] conteos en BD
    boolean existsByUsuarioIdAndFecha(Integer usuarioId, LocalDate fecha);
    long countByEstadoAsistencia(String estadoAsistencia);
}
