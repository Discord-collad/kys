package principio.kys.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(1)
public class SyncSequences implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public SyncSequences(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        for (String tabla : new String[]{"usuarios", "asistencias", "roles", "horarios", "cursos",
                "horario_usuario", "curso_usuario"}) {
            sincronizar(tabla);
        }
    }

    private void sincronizar(String tabla) {
        try {
            String secuencia = jdbcTemplate.queryForObject(
                    "SELECT pg_get_serial_sequence(?, 'id')", String.class, tabla);
            if (secuencia != null) {
                String sql = "SELECT setval('" + secuencia
                        + "', COALESCE((SELECT MAX(id) FROM " + tabla + "), 0) + 1, false)";
                jdbcTemplate.execute(sql);
            }
        } catch (Exception e) {
            System.err.println(">>> [Secuencias] " + tabla + ": " + e.getMessage());
        }
    }
}
