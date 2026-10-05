package principio.kys.config;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Table;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Validador de arranque: revisa que toda la infraestructura (DB, beans, mappings,
 * templates, rutas) esté coherente antes de servir peticiones.
 *
 * Si encuentra errores, los lista y aborta el arranque.
 * Si todo está OK, loguea un resumen.
 */
// @Component
// @Order(2)
public class StartupValidator implements ApplicationRunner {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ApplicationContext context;
    @PersistenceContext private EntityManager entityManager;

    private final List<String> errores = new ArrayList<>();
    private final List<String> advertencias = new ArrayList<>();

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        System.out.println();
        System.out.println("╔════════════════════════════════════════════════════════════╗");
        System.out.println("║          KYS - VALIDACIÓN DE INTEGRIDAD                   ║");
        System.out.println("╚════════════════════════════════════════════════════════════╝");

        validarConexionDB();
        validarTablasYColumnas();
        validarEntidadesJPA();
        validarBeansSpring();
        validarRutasYPlantillas();
        validarDependencias();

        imprimirResumen();

        if (!errores.isEmpty()) {
            String msg = "La aplicación no puede arrancar. Errores críticos:\n" + String.join("\n", errores);
            throw new IllegalStateException(msg);
        }
    }

    private void validarConexionDB() {
        System.out.println("→ Validando conexión a base de datos…");
        try {
            Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            if (one == null || one != 1) {
                errores.add("DB: la query SELECT 1 devolvió " + one);
            } else {
                System.out.println("  ✓ Conexión OK");
            }
        } catch (Exception e) {
            errores.add("DB: no se pudo conectar (" + e.getMessage() + ")");
        }
    }

    private void validarTablasYColumnas() {
        System.out.println("→ Validando tablas y columnas…");
        Map<String, Map<String, String>> schema = obtenerSchema();
        if (schema.isEmpty()) {
            errores.add("DB: no se pudo leer el esquema (¿permisos insuficientes?)");
            return;
        }

        Set<String> tablasRequeridas = new HashSet<>(Arrays.asList(
                "usuarios", "roles", "asistencias", "horarios", "cursos",
                "horario_usuario", "curso_usuario"
        ));

        Map<String, Set<String>> columnasRequeridas = new HashMap<>();
        columnasRequeridas.put("usuarios", new HashSet<>(Arrays.asList(
                "id", "nombre", "apellido", "email", "username", "password_hash", "estado", "rol_id"
        )));
        columnasRequeridas.put("roles", new HashSet<>(Arrays.asList(
                "id", "nombre"
        )));
        columnasRequeridas.put("asistencias", new HashSet<>(Arrays.asList(
                "id", "usuario_id", "fecha", "hora_entrada", "hora_salida",
                "estado_asistencia", "observaciones", "validado_por_id", "hora_validacion"
        )));
        columnasRequeridas.put("horarios", new HashSet<>(Arrays.asList(
                "id", "nombre", "hora_entrada", "hora_salida", "tolerancia_minutos"
        )));
        columnasRequeridas.put("cursos", new HashSet<>(Arrays.asList(
                "id", "nombre", "turno", "anio"
        )));
        columnasRequeridas.put("horario_usuario", new HashSet<>(Arrays.asList(
                "usuario_id", "horario_id"
        )));
        columnasRequeridas.put("curso_usuario", new HashSet<>(Arrays.asList(
                "curso_id", "usuario_id"
        )));

        for (String tabla : tablasRequeridas) {
            if (!schema.containsKey(tabla)) {
                errores.add("DB: falta la tabla '" + tabla + "'");
                continue;
            }
            Set<String> cols = columnasRequeridas.getOrDefault(tabla, Set.of());
            Set<String> existentes = new HashSet<>();
            for (String col : schema.get(tabla).keySet()) {
                existentes.add(col.toLowerCase());
            }
            for (String col : cols) {
                if (!existentes.contains(col.toLowerCase())) {
                    errores.add("DB: tabla '" + tabla + "' falta la columna '" + col + "'");
                }
            }
        }

        if (errores.stream().noneMatch(e -> e.startsWith("DB:"))) {
            System.out.println("  ✓ " + tablasRequeridas.size() + " tablas OK");
        }
    }

    private Map<String, Map<String, String>> obtenerSchema() {
        Map<String, Map<String, String>> schema = new HashMap<>();
        try {
            jdbcTemplate.query("""
                SELECT table_name, column_name, data_type
                FROM information_schema.columns
                WHERE table_schema = current_schema()
                """, rs -> {
                    String tabla = rs.getString("table_name").toLowerCase();
                    String columna = rs.getString("column_name").toLowerCase();
                    String tipo = rs.getString("data_type");
                    schema.computeIfAbsent(tabla, k -> new HashMap<>()).put(columna, tipo);
                });
        } catch (Exception e) {
            errores.add("DB: error leyendo information_schema (" + e.getMessage() + ")");
        }
        return schema;
    }

    private void validarEntidadesJPA() {
        System.out.println("→ Validando entidades JPA…");
        Map<String, String> entidades = new HashMap<>();
        for (Object bean : context.getBeansWithAnnotation(Entity.class).values()) {
            Class<?> clazz = bean.getClass();
            Entity ann = clazz.getAnnotation(Entity.class);
            String nombre = ann != null && !ann.name().isEmpty() ? ann.name() : clazz.getSimpleName();
            Table tableAnn = clazz.getAnnotation(Table.class);
            String tabla = tableAnn != null && !tableAnn.name().isEmpty()
                    ? tableAnn.name() : nombre.toLowerCase();
            entidades.put(clazz.getSimpleName(), tabla);
        }

        for (Map.Entry<String, String> e : entidades.entrySet()) {
            try {
                Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + e.getValue(), Long.class);
                if (count == null) {
                    advertencias.add("JPA: entity " + e.getKey() + " → tabla '" + e.getValue() + "' COUNT devolvió null");
                } else {
                    System.out.println("  ✓ " + e.getKey() + " → " + e.getValue() + " (" + count + " filas)");
                }
            } catch (Exception ex) {
                errores.add("JPA: entity " + e.getKey() + " → no se puede consultar la tabla '"
                        + e.getValue() + "' (" + ex.getMessage() + ")");
            }
        }
    }

    private void validarBeansSpring() {
        System.out.println("→ Validando beans de Spring…");
        Map<String, Object> controllers = context.getBeansWithAnnotation(Controller.class);
        Map<String, Object> restControllers = context.getBeansWithAnnotation(RestController.class);
        Map<String, Object> services = context.getBeansWithAnnotation(Service.class);
        Map<String, Object> repos = context.getBeansWithAnnotation(Repository.class);

        System.out.println("  ✓ Controllers: " + controllers.size());
        System.out.println("  ✓ RestControllers: " + restControllers.size());
        System.out.println("  ✓ Services: " + services.size());
        System.out.println("  ✓ Repositories: " + repos.size());

        if (controllers.isEmpty() && restControllers.isEmpty()) {
            errores.add("Spring: no se detectó ningún @Controller o @RestController");
        }
        if (services.isEmpty()) {
            advertencias.add("Spring: no se detectó ningún @Service");
        }
        if (repos.isEmpty()) {
            errores.add("Spring: no se detectó ningún @Repository");
        }

        for (Object c : controllers.values()) {
            verificarInyecciones(c.getClass());
        }
        for (Object rc : restControllers.values()) {
            verificarInyecciones(rc.getClass());
        }
    }

    private void verificarInyecciones(Class<?> clazz) {
        for (Field f : clazz.getDeclaredFields()) {
            org.springframework.beans.factory.annotation.Autowired a = f.getAnnotation(org.springframework.beans.factory.annotation.Autowired.class);
            if (a == null) continue;
            String nombreBean = Character.toLowerCase(f.getType().getSimpleName().charAt(0))
                    + f.getType().getSimpleName().substring(1);
            try {
                if (!context.containsBean(nombreBean)) {
                    errores.add("Inyección: " + clazz.getSimpleName() + "." + f.getName()
                            + " → no existe bean '" + nombreBean + "'");
                }
            } catch (Exception e) {
                advertencias.add("Inyección: " + clazz.getSimpleName() + "." + f.getName() + " → " + e.getMessage());
            }
        }
    }

    private void validarRutasYPlantillas() {
        System.out.println("→ Validando rutas MVC y plantillas…");
        Set<String> plantillas = listarPlantillas();
        Set<String> rutas = new HashSet<>();
        Set<String> rutasVistas = new HashSet<>();

        for (Object c : context.getBeansWithAnnotation(Controller.class).values()) {
            Class<?> clazz = c.getClass();
            String prefijo = "";
            RequestMapping rmClase = clazz.getAnnotation(RequestMapping.class);
            if (rmClase != null && rmClase.value().length > 0) {
                prefijo = rmClase.value()[0];
            }
            for (Method m : clazz.getDeclaredMethods()) {
                RequestMapping rm = m.getAnnotation(RequestMapping.class);
                GetMapping gm = m.getAnnotation(GetMapping.class);
                PostMapping pm = m.getAnnotation(PostMapping.class);
                String[] values = null;
                String verbo = "GET";
                if (rm != null && rm.value().length > 0) { values = rm.value(); verbo = "ANY"; }
                else if (gm != null && gm.value().length > 0) { values = gm.value(); verbo = "GET"; }
                else if (pm != null && pm.value().length > 0) { values = pm.value(); verbo = "POST"; }
                if (values == null) continue;
                for (String v : values) {
                    String ruta = (prefijo + v).replaceAll("//+", "/");
                    rutas.add(verbo + " " + ruta);
                    String vista = inferirVista(m, ruta);
                    if (vista != null) {
                        rutasVistas.add(vista);
                        if (!plantillas.contains(vista)) {
                            errores.add("Ruta: " + verbo + " " + ruta + " → falta plantilla '" + vista + ".html'");
                        }
                    }
                }
            }
        }

        for (String p : plantillas) {
            if (!rutasVistas.contains(p)) {
                advertencias.add("Plantilla '" + p + ".html' no es referenciada por ningún controller");
            }
        }

        System.out.println("  ✓ Rutas detectadas: " + rutas.size());
        System.out.println("  ✓ Plantillas: " + plantillas.size());
    }

    private Set<String> listarPlantillas() {
        Set<String> out = new TreeSet<>();
        try {
            PathMatchingResourcePatternResolver r = new PathMatchingResourcePatternResolver();
            Resource[] resources = r.getResources("classpath:/templates/**/*.html");
            for (Resource res : resources) {
                String url = res.getURL().getPath();
                String plantilla = url.replaceAll(".*templates[\\\\/](.+)\\.html$", "$1").replace('\\', '/');
                out.add(plantilla);
            }
        } catch (Exception e) {
            advertencias.add("No se pudieron listar plantillas: " + e.getMessage());
        }
        return out;
    }

    private String inferirVista(Method m, String ruta) {
        if (m.getReturnType() == String.class) {
            String nombre = m.getName();
            if (nombre.startsWith("home") || nombre.equals("index")) return "index";
            if (nombre.contains("Form")) {
                String base = nombre.replace("Form", "").toLowerCase();
                return base + "/" + base + "-form";
            }
        }
        return null;
    }

    private void validarDependencias() {
        System.out.println("→ Validando referencias entre clases…");
        try {
            for (Object s : context.getBeansWithAnnotation(Service.class).values()) {
                verificarInyecciones(s.getClass());
            }
        } catch (Exception e) {
            advertencias.add("Dependencias: " + e.getMessage());
        }
    }

    private void imprimirResumen() {
        System.out.println();
        System.out.println("════════════════════════ RESUMEN ════════════════════════════");
        if (errores.isEmpty()) {
            System.out.println("✓ Sin errores. Todo está coherente.");
        } else {
            System.out.println("✗ ERRORES (" + errores.size() + "):");
            for (String e : errores) {
                System.out.println("   • " + e);
            }
        }
        if (!advertencias.isEmpty()) {
            System.out.println("⚠ ADVERTENCIAS (" + advertencias.size() + "):");
            for (String w : advertencias) {
                System.out.println("   • " + w);
            }
        }
        System.out.println("══════════════════════════════════════════════════════════════");
        System.out.println();
    }
}
