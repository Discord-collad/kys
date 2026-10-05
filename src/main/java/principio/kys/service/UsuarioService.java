package principio.kys.service;

import principio.kys.model.Rol;
import principio.kys.model.Usuario;
import principio.kys.repository.AsistenciaRepository;
import principio.kys.repository.RolRepository;
import principio.kys.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class UsuarioService {
    private final UsuarioRepository usuarioRepo;
    private final RolRepository rolRepo;
    private final AsistenciaRepository asistenciaRepo;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public UsuarioService(UsuarioRepository usuarioRepo, RolRepository rolRepo,
                          AsistenciaRepository asistenciaRepo, PasswordEncoder passwordEncoder) {
        this.usuarioRepo = usuarioRepo;
        this.rolRepo = rolRepo;
        this.asistenciaRepo = asistenciaRepo;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario registrar(@NonNull Usuario usuario, String passwordPlana) {
        if (passwordPlana != null && !passwordPlana.isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(passwordPlana.trim()));
        }
        return usuarioRepo.save(usuario);
    }

    public Optional<Usuario> login(String username, String passwordPlana) {
        if (username == null || passwordPlana == null) return Optional.empty();
        return usuarioRepo.findByUsernameIgnoreCase(username.trim())
                .filter(u -> u.getPasswordHash() != null
                        && passwordEncoder.matches(passwordPlana.trim(), u.getPasswordHash()));
    }

    public List<Usuario> listarTodos() { return usuarioRepo.findAll(); }

    public Usuario buscarPorId(@NonNull Integer id) {
        return usuarioRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + id));
    }

    public Optional<Usuario> buscarPorIdOpt(@NonNull Integer id) { return usuarioRepo.findById(id); }

    public Usuario buscarPorUsername(@NonNull String username) {
        return usuarioRepo.findByUsernameIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + username));
    }

    public void desactivar(@NonNull Integer id) {
        usuarioRepo.findById(id).ifPresent(u -> {
            u.setEstado(false);
            usuarioRepo.save(u);
        });
    }

    @Transactional
    @SuppressWarnings("null")
    public boolean eliminar(Integer id) {
        if (!usuarioRepo.existsById(id)) return false;
        asistenciaRepo.deleteByUsuarioId(id);
        usuarioRepo.deleteById(id);
        return true;
    }

    public Usuario registrarUsuarioPublico(Usuario usuario, String passwordPlana) {
        Rol rolAlumno = rolRepo.findByNombre("ALUMNO").orElseGet(() -> {
            Rol nuevo = new Rol();
            nuevo.setNombre("ALUMNO");
            return rolRepo.save(nuevo);
        });
        usuario.setRol(rolAlumno);
        usuario.setEstado(true);
        return registrar(usuario, passwordPlana);
    }

    public Rol obtenerOCrearRol(String nombre, String nombreLegacy) {
        Optional<Rol> existente = rolRepo.findByNombre(nombre);
        if (existente.isPresent()) return existente.get();
        if (nombreLegacy != null) {
            Optional<Rol> legacy = rolRepo.findByNombre(nombreLegacy);
            if (legacy.isPresent()) {
                Rol r = legacy.get();
                r.setNombre(nombre);
                return rolRepo.save(r);
            }
        }
        Rol nuevo = new Rol();
        nuevo.setNombre(nombre);
        return rolRepo.save(nuevo);
    }

    public Optional<Usuario> buscarPorUsernameOpt(String username) {
        return usuarioRepo.findByUsernameIgnoreCase(username);
    }

    public Optional<Usuario> buscarPorEmailOpt(String email) {
        return usuarioRepo.findByEmailIgnoreCase(email);
    }
}
