package principio.kys.config;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import principio.kys.model.Usuario;
import principio.kys.repository.UsuarioRepository;

import java.util.Random;

/**
 * Expone a todas las vistas Thymeleaf los datos comunes de navegación:
 * - usuarioActual: entidad Usuario real del usuario autenticado (o null si no hay sesión)
 * - usernameActual: username de la sesión
 * - rolActual: DIRECTOR / PROFESOR / ALUMNO (o "" si no hay sesión)
 * - homeUrl: ruta de inicio según el rol (rutas reales del proyecto)
 * - nombreParaMostrar: nombre real si existe, si no el username
 * - inicialUsuario: inicial para el avatar
 * - saludoAleatorio: frase de bienvenida local (sin API externa ni BD extra)
 *
 * No expone passwordHash ni datos sensibles.
 */
@ControllerAdvice
public class GlobalModelAdvice {

    private static final String[] SALUDOS = {
            "¡Bienvenido, %s!",
            "¡Qué gusto verte, %s!",
            "¡Hola, %s! Todo listo.",
            "¡Buen día, %s!",
            "¡Bienvenido de nuevo, %s!",
            "¡Hola, %s! Empecemos.",
            "¡Todo preparado, %s!"
    };

    private static final String SALUDO_GENERICO = "¡Hola! Todo listo.";

    private final UsuarioRepository usuarioRepository;
    private final Random random = new Random();

    public GlobalModelAdvice(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @ModelAttribute
    public void agregarDatosGlobales(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean autenticado = auth != null && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken)
                && auth.getPrincipal() instanceof UserDetails;
        String username = (autenticado && auth != null) ? auth.getName() : null;

        Usuario usuario = null;
        if (username != null) {
            try {
                usuario = usuarioRepository.findByUsernameIgnoreCase(username).orElse(null);
            } catch (Exception e) {
                // Si la consulta falla, se muestra saludo genérico en lugar de romper la vista.
                usuario = null;
            }
        }

        String rol = rolDe(usuario, auth, autenticado);

        String nombreParaMostrar = nombreParaMostrar(usuario, username);
        String saludo = nombreParaMostrar == null
                ? SALUDO_GENERICO
                : String.format(SALUDOS[random.nextInt(SALUDOS.length)], nombreParaMostrar);

        model.addAttribute("usuarioActual", usuario);
        model.addAttribute("usernameActual", username);
        model.addAttribute("rolActual", rol);
        model.addAttribute("homeUrl", homeUrlPorRol(rol));
        model.addAttribute("nombreParaMostrar",
                nombreParaMostrar != null ? nombreParaMostrar : "Usuario");
        model.addAttribute("inicialUsuario", inicialDe(nombreParaMostrar));
        model.addAttribute("saludoAleatorio", saludo);
    }

    private String rolDe(Usuario usuario, Authentication auth, boolean autenticado) {
        if (usuario != null && usuario.getRol() != null && usuario.getRol().getNombre() != null) {
            return usuario.getRol().getNombre();
        }
        if (autenticado && auth != null) {
            return auth.getAuthorities().stream()
                    .map(a -> a.getAuthority())
                    .filter(a -> a.startsWith("ROLE_"))
                    .findFirst()
                    .map(a -> a.substring("ROLE_".length()))
                    .orElse("");
        }
        return "";
    }

    private String nombreParaMostrar(Usuario usuario, String username) {
        if (usuario != null && usuario.getNombre() != null && !usuario.getNombre().isBlank()) {
            return usuario.getNombre().trim();
        }
        if (username != null && !username.isBlank()) {
            return username;
        }
        return null;
    }

    private String inicialDe(String nombre) {
        if (nombre == null || nombre.isBlank()) return "?";
        return nombre.substring(0, 1).toUpperCase();
    }

    private String homeUrlPorRol(String rol) {
        if ("DIRECTOR".equals(rol)) return "/admin";
        if ("PROFESOR".equals(rol)) return "/profesor";
        if ("ALUMNO".equals(rol)) return "/alumno";
        return "/login";
    }
}
