package principio.kys.config;

import principio.kys.service.PresenciaService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;

@Component
public class SesionListener implements HttpSessionListener {

    private final PresenciaService presencia;

    public SesionListener(PresenciaService presencia) {
        this.presencia = presencia;
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent evento) {
        Object contexto = evento.getSession().getAttribute("SPRING_SECURITY_CONTEXT");
        if (contexto instanceof SecurityContext seguridad) {
            Authentication autenticacion = seguridad.getAuthentication();
            if (autenticacion != null && autenticacion.isAuthenticated()) {
                presencia.desconectar(autenticacion.getName());
            }
        }
    }
}
