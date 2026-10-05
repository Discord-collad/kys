package principio.kys.config;

import principio.kys.model.Curso;
import principio.kys.model.Horario;
import principio.kys.model.Rol;
import principio.kys.model.Usuario;
import principio.kys.repository.CursoRepository;
import principio.kys.repository.HorarioRepository;
import principio.kys.repository.RolRepository;
import principio.kys.repository.UsuarioRepository;
import principio.kys.service.UsuarioService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Component
public class MigracionPasswords implements ApplicationRunner {

    private final UsuarioRepository usuarioRepo;
    private final RolRepository rolRepo;
    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;
    private final HorarioRepository horarioRepo;
    private final CursoRepository cursoRepo;

    public MigracionPasswords(UsuarioRepository usuarioRepo, RolRepository rolRepo,
                              UsuarioService usuarioService,
                              PasswordEncoder passwordEncoder,
                              HorarioRepository horarioRepo, CursoRepository cursoRepo) {
        this.usuarioRepo = usuarioRepo;
        this.rolRepo = rolRepo;
        this.usuarioService = usuarioService;
        this.passwordEncoder = passwordEncoder;
        this.horarioRepo = horarioRepo;
        this.cursoRepo = cursoRepo;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        migrarRoles();
        sembrarDatosDemo();
        migrarPasswords();
    }

    private void migrarRoles() {
        Rol director = usuarioService.obtenerOCrearRol("DIRECTOR", "ADMIN");
        usuarioService.obtenerOCrearRol("PROFESOR", null);
        Rol alumno = usuarioService.obtenerOCrearRol("ALUMNO", "USER");

        Integer directorId = director.getId();
        Integer alumnoId = alumno.getId();

        Optional<Rol> legacyAdmin = rolRepo.findByNombre("ADMIN");
        if (legacyAdmin.isPresent() && directorId != null && !directorId.equals(legacyAdmin.get().getId())) {
            Rol legacy = legacyAdmin.get();
            usuarioRepo.findAll().stream()
                    .filter(u -> u.getRol() != null && legacy.getId().equals(u.getRol().getId()))
                    .forEach(u -> { u.setRol(director); usuarioRepo.save(u); });
        }

        Optional<Rol> legacyUser = rolRepo.findByNombre("USER");
        if (legacyUser.isPresent() && alumnoId != null && !alumnoId.equals(legacyUser.get().getId())) {
            Rol legacy = legacyUser.get();
            usuarioRepo.findAll().stream()
                    .filter(u -> u.getRol() != null && legacy.getId().equals(u.getRol().getId()))
                    .forEach(u -> { u.setRol(alumno); usuarioRepo.save(u); });
        }

        List<Usuario> adminUser = usuarioRepo.findAll().stream()
                .filter(u -> u.getUsername() != null && u.getUsername().equalsIgnoreCase("admin"))
                .toList();
        for (Usuario u : adminUser) {
            u.setUsername("director");
            usuarioRepo.save(u);
        }
    }

    private void sembrarDatosDemo() {
        if (horarioRepo.count() == 0) {
            Horario manana = new Horario();
            manana.setNombre("Turno Mañana");
            manana.setHoraEntrada(LocalTime.of(8, 0));
            manana.setHoraSalida(LocalTime.of(12, 0));
            manana.setToleranciaMinutos(15);
            horarioRepo.save(manana);

            Horario tarde = new Horario();
            tarde.setNombre("Turno Tarde");
            tarde.setHoraEntrada(LocalTime.of(14, 0));
            tarde.setHoraSalida(LocalTime.of(18, 0));
            tarde.setToleranciaMinutos(15);
            horarioRepo.save(tarde);
        }
        if (cursoRepo.count() == 0) {
            Curso c1 = new Curso();
            c1.setNombre("1° Año A");
            c1.setTurno("Mañana");
            c1.setAnio("2026");
            cursoRepo.save(c1);

            Curso c2 = new Curso();
            c2.setNombre("2° Año B");
            c2.setTurno("Tarde");
            c2.setAnio("2026");
            cursoRepo.save(c2);
        }

        Rol rolProfesor = usuarioService.obtenerOCrearRol("PROFESOR", null);
        if (usuarioRepo.findByUsernameIgnoreCase("profesor").isEmpty()) {
            Usuario p = new Usuario();
            p.setNombre("Carlos");
            p.setApellido("Gómez");
            p.setEmail("profesor@kys.com");
            p.setUsername("profesor");
            p.setPasswordHash(passwordEncoder.encode("profesor123"));
            p.setEstado(true);
            p.setRol(rolProfesor);
            usuarioRepo.save(p);
        }

        Rol rolAlumno = usuarioService.obtenerOCrearRol("ALUMNO", null);
        if (usuarioRepo.findByUsernameIgnoreCase("alumno").isEmpty()) {
            Usuario a = new Usuario();
            a.setNombre("Lucía");
            a.setApellido("Martínez");
            a.setEmail("alumno@kys.com");
            a.setUsername("alumno");
            a.setPasswordHash(passwordEncoder.encode("alumno123"));
            a.setEstado(true);
            a.setRol(rolAlumno);
            usuarioRepo.save(a);
        }
    }

    private void migrarPasswords() {
        List<Usuario> usuarios = usuarioRepo.findAll();
        int migrados = 0;
        for (Usuario u : usuarios) {
            String hash = u.getPasswordHash();
            if (hash != null && !hash.startsWith("$2")) {
                u.setPasswordHash(passwordEncoder.encode(hash));
                usuarioRepo.save(u);
                migrados++;
            }
        }
        System.out.println(">>> [Migracion] Contraseñas convertidas a BCrypt: " + migrados);
    }
}

