package principio.kys.controller;

import principio.kys.model.Usuario;
import principio.kys.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {
    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // [P3] Solo DIRECTOR: la API no se usa desde el frontend y expone la lista completa de usuarios.
    @GetMapping
    @PreAuthorize("hasRole('DIRECTOR')")
    public List<Usuario> listar() { return usuarioService.listarTodos(); }

    // Registro API: ignora rol enviado y fuerza ALUMNO (evita escalada de privilegios).
    // [P0] La contraseña ya NO se acepta por query (?password quedaba en logs/historial): solo body.
    @PostMapping("/registro")
    public ResponseEntity<?> registrar(@RequestBody(required = false) Map<String, Object> body) {
        if (body == null) return ResponseEntity.badRequest().body("Falta body JSON con usuario");
        Usuario usuario = new Usuario();
        if (body.get("nombre") != null) usuario.setNombre(String.valueOf(body.get("nombre")));
        if (body.get("apellido") != null) usuario.setApellido(String.valueOf(body.get("apellido")));
        if (body.get("username") != null) usuario.setUsername(String.valueOf(body.get("username")));
        if (body.get("email") != null) usuario.setEmail(String.valueOf(body.get("email")));
        String pass = body.get("password") != null ? String.valueOf(body.get("password")) : null;
        if (pass == null || pass.isBlank()) {
            return ResponseEntity.badRequest().body("Falta password en el body JSON");
        }
        // [P2] Validaciones básicas server-side (sin depender del frontend)
        if (usuario.getUsername() == null || usuario.getUsername().isBlank()
                || usuario.getEmail() == null || usuario.getEmail().isBlank()) {
            return ResponseEntity.badRequest().body("Faltan username o email");
        }
        usuario.setUsername(usuario.getUsername().trim());
        usuario.setEmail(usuario.getEmail().trim());
        if (usuario.getUsername().length() < 4) {
            return ResponseEntity.badRequest().body("El username debe tener al menos 4 caracteres");
        }
        if (!usuario.getEmail().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            return ResponseEntity.badRequest().body("Email inválido");
        }
        if (usuarioService.buscarPorUsernameOpt(usuario.getUsername()).isPresent()) {
            return ResponseEntity.status(409).body("Ese username ya está en uso");
        }
        if (usuarioService.buscarPorEmailOpt(usuario.getEmail()).isPresent()) {
            return ResponseEntity.status(409).body("Ese email ya está registrado");
        }
        return ResponseEntity.ok(usuarioService.registrarUsuarioPublico(usuario, pass));
    }

    // Login API: acepta JSON {username,password} (recomendado, no queda en logs) o ?params legacy.
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody(required = false) Map<String, String> body,
                                   @RequestParam(required = false) String username,
                                   @RequestParam(required = false) String password) {
        String u = username;
        String p = password;
        if (body != null) {
            if (body.get("username") != null) u = body.get("username");
            if (body.get("password") != null) p = body.get("password");
        }
        if (u == null || p == null) {
            return ResponseEntity.badRequest().body("Falta username/password");
        }
        final String uf = u.trim();
        final String pf = p.trim();
        return usuarioService.login(uf, pf)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(401).body("Credenciales inválidas"));
    }

    @PostMapping("/crear-director")
    @PreAuthorize("hasRole('DIRECTOR')")
    public ResponseEntity<?> crearDirector() {
        usuarioService.obtenerOCrearRol("DIRECTOR", null);
        usuarioService.buscarPorUsernameOpt("director").ifPresentOrElse(
                d -> { },
                () -> {
                    Usuario director = new Usuario();
                    director.setNombre("Director");
                    director.setApellido("Principal");
                    director.setEmail("director@kys.com");
                    director.setUsername("director");
                    director.setEstado(true);
                    director.setRol(usuarioService.obtenerOCrearRol("DIRECTOR", null));
                    usuarioService.registrar(director, "director123");
                }
        );
        return ResponseEntity.ok("Director creado/actualizado con rol DIRECTOR");
    }
}
