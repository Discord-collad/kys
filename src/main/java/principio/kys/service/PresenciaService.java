package principio.kys.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PresenciaService {

    private final Set<String> usuariosEnLinea = ConcurrentHashMap.newKeySet();

    public void conectar(String username) {
        if (username != null) {
            usuariosEnLinea.add(username);
        }
    }

    public void desconectar(String username) {
        if (username != null) {
            usuariosEnLinea.remove(username);
        }
    }

    public boolean estaEnLinea(String username) {
        return usuariosEnLinea.contains(username);
    }

    public Set<String> listarEnLinea() {
        return Collections.unmodifiableSet(usuariosEnLinea);
    }
}
