package principio.kys.repository;

import principio.kys.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByUsername(String username);
    Optional<Usuario> findByUsernameIgnoreCase(String username);
    Optional<Usuario> findByEmail(String email);
    Optional<Usuario> findByEmailIgnoreCase(String email);

    // [P3] Conteo por rol directo en BD (reemplaza findAll().stream().filter().count())
    long countByRolNombre(String nombreRol);
}
