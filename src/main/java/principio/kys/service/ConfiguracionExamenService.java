package principio.kys.service;

import principio.kys.model.ConfiguracionExamen;
import principio.kys.repository.ConfiguracionExamenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ConfiguracionExamenService {
    private final ConfiguracionExamenRepository configRepo;

    @Autowired
    public ConfiguracionExamenService(ConfiguracionExamenRepository configRepo) {
        this.configRepo = configRepo;
    }

    public Optional<ConfiguracionExamen> obtenerConfiguracionActual() {
        return Optional.ofNullable(configRepo.findFirstByOrderByIdDesc());
    }

    public ConfiguracionExamen obtenerOCrearConfiguracion() {
        return obtenerConfiguracionActual().orElseGet(() -> {
            ConfiguracionExamen nueva = new ConfiguracionExamen();
            return configRepo.save(nueva);
        });
    }

    @SuppressWarnings("null")
    public ConfiguracionExamen guardar(ConfiguracionExamen config) {
        return configRepo.save(config);
    }

    @Transactional
    public void activarSemanaExamenes() {
        ConfiguracionExamen config = obtenerOCrearConfiguracion();
        config.setSemanaExamenesActiva(true);
        configRepo.save(config);
    }

    @Transactional
    public void desactivarSemanaExamenes() {
        ConfiguracionExamen config = obtenerOCrearConfiguracion();
        config.setSemanaExamenesActiva(false);
        configRepo.save(config);
    }

    @SuppressWarnings("null")
    public boolean estaActivaSemanaExamenes() {
        return obtenerConfiguracionActual()
                .map(ConfiguracionExamen::getSemanaExamenesActiva)
                .orElse(false);
    }

    @SuppressWarnings("null")
    public boolean debeMostrarAAumnos() {
        return obtenerConfiguracionActual()
                .map(ConfiguracionExamen::getMostrarAAumnos)
                .orElse(true);
    }

    @SuppressWarnings("null")
    public boolean debeMostrarAProfesores() {
        return obtenerConfiguracionActual()
                .map(ConfiguracionExamen::getMostrarAProfesores)
                .orElse(true);
    }
}
