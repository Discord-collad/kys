package principio.kys.repository;

import principio.kys.model.PeriodoLectivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.Optional;

public interface PeriodoLectivoRepository extends JpaRepository<PeriodoLectivo, Integer> {
    Optional<PeriodoLectivo> findByActivoTrue();
    Optional<PeriodoLectivo> findByEsUltimoPeriodoTrue();

    /** Deja un solo periodo activo: apaga el flag en todas las filas de una. */
    @Modifying
    @Query("UPDATE PeriodoLectivo p SET p.activo = false WHERE p.activo = true")
    void desactivarTodos();

    /** Deja un solo "último periodo": apaga el flag en todas las filas de una. */
    @Modifying
    @Query("UPDATE PeriodoLectivo p SET p.esUltimoPeriodo = false WHERE p.esUltimoPeriodo = true")
    void desmarcarUltimoDeTodos();
}
