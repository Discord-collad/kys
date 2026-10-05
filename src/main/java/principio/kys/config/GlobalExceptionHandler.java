package principio.kys.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

/**
 * [P2] Manejo global de errores sin stack trace al usuario.
 * - /api/** : respuestas JSON planas (400/409/500).
 * - Resto de rutas web: página error.html amable con homeUrl según el rol.
 * - Las excepciones de Spring Security (AccessDenied) NO se capturan: dejan que
 *   el AccessDeniedHandler de SecurityConfig responda (403 o redirect).
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static boolean esApi(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/api/");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public Object manejarArgumentoInvalido(IllegalArgumentException e, HttpServletRequest request) {
        log.warn("[400] {} : {}", request.getRequestURI(), e.getMessage());
        if (esApi(request)) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
        return paginaError(400, e.getMessage());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)    public Object manejarDuplicado(DataIntegrityViolationException e, HttpServletRequest request) {
        log.warn("[409] {} : {}", request.getRequestURI(), e.getMessage());
        if (esApi(request)) {
            return ResponseEntity.status(409).body(Map.of("error", "El registro ya existe (duplicado)"));
        }
        return paginaError(409, "La operación generó un registro duplicado o viola una regla de datos.");
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public Object manejarParametroInvalido(Exception e, HttpServletRequest request) {
        String detalle = e instanceof MissingServletRequestParameterException miss
                ? "Falta el parámetro obligatorio: " + miss.getParameterName()
                : "Un parámetro tiene un formato inválido.";
        log.warn("[400] {} : {}", request.getRequestURI(), detalle);
        if (esApi(request)) {
            return ResponseEntity.badRequest().body(Map.of("error", detalle));
        }
        return paginaError(400, detalle + " Revisá el formulario e intentá de nuevo.");
    }

    @ExceptionHandler(Exception.class)
    public Object manejarGenerico(Exception e, HttpServletRequest request) throws Exception {
        // Spring Security lanza estos para AccessDenied dentro del controller; no los tapar con 500.
        if (e instanceof AccessDeniedException) {
            throw e;
        }
        log.error("[500] {} : {}", request.getRequestURI(), e.getMessage(), e);
        if (esApi(request)) {
            return ResponseEntity.status(500).body(Map.of("error", "Error interno del servidor"));
        }
        return paginaError(500, "Ocurrió un problema inesperado. Probá nuevamente en unos momentos.");
    }

    private ModelAndView paginaError(int codigo, String mensaje) {
        ModelAndView mv = new ModelAndView("error");
        mv.addObject("codigoError", codigo);
        mv.addObject("mensajeError", mensaje);
        return mv;
    }
}
