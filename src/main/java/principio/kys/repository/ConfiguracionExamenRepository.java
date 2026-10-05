package principio.kys.repository;

import principio.kys.model.ConfiguracionExamen;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfiguracionExamenRepository extends JpaRepository<ConfiguracionExamen, Integer> {
    ConfiguracionExamen findFirstByOrderByIdDesc();
}
