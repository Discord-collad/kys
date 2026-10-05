package principio.kys.repository;

import principio.kys.model.HorarioUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface HorarioUsuarioRepository extends JpaRepository<HorarioUsuario, HorarioUsuario.HorarioUsuarioId> {
    List<HorarioUsuario> findByUsuarioId(Integer usuarioId);
    Optional<HorarioUsuario> findFirstByUsuarioId(Integer usuarioId);

    @Modifying
    @Query("DELETE FROM HorarioUsuario hu WHERE hu.usuarioId = :usuarioId")
    void deleteByUsuarioId(@Param("usuarioId") Integer usuarioId);
}
