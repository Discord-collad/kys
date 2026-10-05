package principio.kys.repository;

import principio.kys.model.CursoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface CursoUsuarioRepository extends JpaRepository<CursoUsuario, CursoUsuario.CursoUsuarioId> {
    List<CursoUsuario> findByUsuarioId(Integer usuarioId);
    List<CursoUsuario> findByCursoId(Integer cursoId);

    @Modifying
    @Query("DELETE FROM CursoUsuario cu WHERE cu.usuarioId = :usuarioId")
    void deleteByUsuarioId(@Param("usuarioId") Integer usuarioId);
}
