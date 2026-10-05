package principio.kys.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;
import org.springframework.validation.BindingResult;
import principio.kys.model.Asistencia;
import principio.kys.model.Rol;
import principio.kys.model.Usuario;
import principio.kys.repository.AsistenciaRepository;
import principio.kys.repository.RolRepository;
import principio.kys.repository.UsuarioRepository;
import principio.kys.service.AsistenciaService;
import principio.kys.service.HorarioService;
import principio.kys.service.PresenciaService;
import principio.kys.service.UsuarioService;
import principio.kys.service.ReporteCsvService;
import principio.kys.service.ConfiguracionExamenService;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
public class HomeController {

    private final UsuarioRepository usuarioRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final RolRepository rolRepository;
    private final UsuarioService usuarioService;
    private final AsistenciaService asistenciaService;
    private final HorarioService horarioService;
    private final PresenciaService presenciaService;
    private final ReporteCsvService reporteCsvService;
    private final ConfiguracionExamenService examenService;

    @Autowired
    public HomeController(UsuarioRepository usuarioRepository, AsistenciaRepository asistenciaRepository,
                          RolRepository rolRepository, UsuarioService usuarioService,
                          AsistenciaService asistenciaService, HorarioService horarioService,
                          PresenciaService presenciaService, ReporteCsvService reporteCsvService,
                          ConfiguracionExamenService examenService) {
        this.usuarioRepository = usuarioRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.rolRepository = rolRepository;
        this.usuarioService = usuarioService;
        this.asistenciaService = asistenciaService;
        this.horarioService = horarioService;
        this.presenciaService = presenciaService;
        this.reporteCsvService = reporteCsvService;
        this.examenService = examenService;
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/admin")
    public String adminPanel(Authentication authentication, Model model) {
        if (authentication == null) return "redirect:/login";
        long totalUsuarios = usuarioRepository.count();
        long asistenciasHoy = asistenciaService.contarAsistenciasHoy();
        long aprobadasHoy = asistenciaService.contarAprobadasHoy();
        long pendientes = asistenciaService.contarPorEstado("PENDIENTE");
        long rechazadasHoy = asistenciaService.contarHoyPorEstado("RECHAZADO");

        // [P3] Conteos por rol directo en BD (antes: findAll() y filtrado en memoria)
        long totalDirectores = usuarioRepository.countByRolNombre("DIRECTOR");
        long totalProfesores = usuarioRepository.countByRolNombre("PROFESOR");
        long totalAlumnos = usuarioRepository.countByRolNombre("ALUMNO");

        model.addAttribute("usuarios", usuarioRepository.findAll());
        model.addAttribute("totalUsuarios", totalUsuarios);
        model.addAttribute("totalDirectores", totalDirectores);
        model.addAttribute("totalProfesores", totalProfesores);
        model.addAttribute("totalAlumnos", totalAlumnos);
        model.addAttribute("totalRoles", rolRepository.count());
        model.addAttribute("asistenciasHoy", asistenciasHoy);
        model.addAttribute("aprobadasHoy", aprobadasHoy);
        model.addAttribute("rechazadasHoy", rechazadasHoy);
        model.addAttribute("pendientes", pendientes);
        model.addAttribute("roles", rolRepository.findAll());
        model.addAttribute("enLinea", presenciaService.listarEnLinea());
        model.addAttribute("enLineaCount", presenciaService.listarEnLinea().size());
        model.addAttribute("rol", "Dirección");
        model.addAttribute("subtitulo", "Panel de control");
        return "director/panel";
    }


    @GetMapping("/alumno")
    public String alumnoPanel(Authentication authentication, Model model) {
        Optional<Usuario> opt = usuarioActual(authentication);
        if (opt.isEmpty()) return "redirect:/login";
        Usuario usuario = opt.get();

        List<Asistencia> asistencias = asistenciaRepository.findByUsuarioId(usuario.getId());
        long minutosTotales = 0;
        Asistencia hoy = null;
        for (Asistencia a : asistencias) {
            if (a.getHoraEntrada() != null && a.getHoraSalida() != null) {
                minutosTotales += Duration.between(a.getHoraEntrada(), a.getHoraSalida()).toMinutes();
            }
            if (LocalDate.now().equals(a.getFecha())) {
                hoy = a;
            }
        }
        model.addAttribute("usuario", usuario);
        model.addAttribute("asistencias", asistencias);
        model.addAttribute("totalAsistencias", asistencias.size());
        model.addAttribute("horasTotales", minutosTotales / 60);
        model.addAttribute("asistenciaHoy", hoy);
        model.addAttribute("configExamen", examenService.obtenerConfiguracionActual().orElse(null));
        model.addAttribute("mostrarAvisoExamenes", examenService.debeMostrarAAumnos());
        model.addAttribute("rol", "Alumno");
        model.addAttribute("subtitulo", "Mi panel de asistencia");
        return "alumno/panel";
    }

    @PostMapping("/alumno/entrada")
    public String marcarEntrada(Authentication authentication, RedirectAttributes redirectAttributes) {
        Optional<Usuario> opt = usuarioActual(authentication);
        if (opt.isEmpty()) return "redirect:/login";
        Usuario usuario = opt.get();

        if (asistenciaService.obtenerDeHoy(usuario.getId()).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Ya registraste tu entrada hoy");
        } else {
            asistenciaService.registrarEntrada(usuario);
            redirectAttributes.addFlashAttribute("mensaje", "Entrada registrada. ¡Bienvenido!");
        }
        return "redirect:/alumno";
    }

    @PostMapping("/alumno/salida")
    public String marcarSalida(Authentication authentication, RedirectAttributes redirectAttributes) {
        Optional<Usuario> opt = usuarioActual(authentication);
        if (opt.isEmpty()) return "redirect:/login";
        Usuario usuario = opt.get();

        try {
            if (asistenciaService.registrarSalida(usuario).isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Primero debés registrar tu entrada");
            } else {
                redirectAttributes.addFlashAttribute("mensaje", "Salida registrada. ¡Hasta mañana!");
            }
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/alumno";
    }

    @GetMapping("/admin/usuarios/nuevo")
    public String nuevoUsuarioForm(Model model) {
        model.addAttribute("usuario", new Usuario());
        model.addAttribute("roles", rolRepository.findAll());
        model.addAttribute("rol", "Dirección");
        model.addAttribute("subtitulo", "Nuevo usuario");
        return "director/usuario-form";
    }

    @PostMapping("/admin/usuarios/guardar")
    @SuppressWarnings("null")
    public String guardarUsuario(@Valid @ModelAttribute Usuario usuario, BindingResult bindingResult,
                                  @RequestParam Integer rolId,
                                  @RequestParam(required = false) String password,
                                  RedirectAttributes redirectAttributes, Model model) {
        // [P2] Validación server-side: re-renderiza el form conservando lo tipeado
        boolean passFalta = usuario.getId() == null && (password == null || password.isBlank());
        if (bindingResult.hasErrors() || passFalta) {
            model.addAttribute("error", passFalta
                    ? "La contraseña es obligatoria para un usuario nuevo"
                    : "Revisá los campos marcados como obligatorios");
            model.addAttribute("roles", rolRepository.findAll());
            return "director/usuario-form";
        }
        Rol rol = rolRepository.findById(rolId)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + rolId));
        usuario.setRol(rol);
        usuarioService.registrar(usuario, password);
        redirectAttributes.addFlashAttribute("mensaje", "Usuario guardado exitosamente");
        return "redirect:/admin";
    }

    @GetMapping("/admin/usuarios/editar/{id}")
    public String editarUsuarioForm(@PathVariable @NonNull Integer id, Model model,
                                    RedirectAttributes redirectAttributes) {
        Optional<Usuario> opt = usuarioService.buscarPorIdOpt(id);
        if (opt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "El usuario solicitado no existe (id " + id + ")");
            return "redirect:/admin";
        }
        model.addAttribute("usuario", opt.get());
        model.addAttribute("roles", rolRepository.findAll());
        model.addAttribute("rol", "Dirección");
        model.addAttribute("subtitulo", "Editar usuario");
        return "director/usuario-form";
    }

    @PostMapping("/admin/usuarios/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            if (usuarioService.eliminar(id)) {
                redirectAttributes.addFlashAttribute("mensaje", "Usuario eliminado exitosamente");
            } else {
                redirectAttributes.addFlashAttribute("error", "El usuario no existe");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "No se pudo eliminar el usuario");
        }
        return "redirect:/admin";
    }

    @GetMapping("/admin/usuarios")
    public String listarUsuarios(Model model) {
        model.addAttribute("usuarios", usuarioRepository.findAll());
        model.addAttribute("roles", rolRepository.findAll());
        model.addAttribute("enLinea", presenciaService.listarEnLinea());
        model.addAttribute("enLineaCount", presenciaService.listarEnLinea().size());
        model.addAttribute("rol", "Dirección");
        model.addAttribute("subtitulo", "Usuarios");
        return "director/usuarios";
    }

    @GetMapping("/admin/asistencias")
    public String listarAsistencias(Model model) {
        model.addAttribute("asistencias", asistenciaRepository.findAll());
        model.addAttribute("usuarios", usuarioRepository.findAll());
        model.addAttribute("rol", "Dirección");
        model.addAttribute("subtitulo", "Gestión de asistencias");
        return "director/asistencias";
    }

    @PostMapping("/admin/asistencias/registrar")
    @SuppressWarnings("null")
    public String registrarAsistencia(@RequestParam @NonNull Integer usuarioId, RedirectAttributes redirectAttributes) {
        usuarioService.buscarPorIdOpt(usuarioId).ifPresent(usuario -> {
            asistenciaService.registrarDirecta(usuario);
            redirectAttributes.addFlashAttribute("mensaje", "Asistencia registrada (aprobada por dirección)");
        });
        return "redirect:/admin/asistencias";
    }

    @PostMapping("/admin/asistencias/salida/{id}")
    public String registrarSalida(@PathVariable Integer id, RedirectAttributes redirectAttributes) {
        try {
            asistenciaService.registrarSalidaDirecta(id).ifPresent(a ->
                    redirectAttributes.addFlashAttribute("mensaje", "Salida registrada"));
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/asistencias";
    }

    /** [P5] Exportación CSV de todas las asistencias (ruta bajo /admin: solo DIRECTOR). */
    @GetMapping("/admin/asistencias/csv")
    public org.springframework.http.ResponseEntity<byte[]> exportarCsvAdmin() {
        String csv = reporteCsvService.asistenciasCsv(asistenciaRepository.findAll());
        return ReporteCsvService.respuesta(csv, "asistencias_todas.csv");
    }

    @GetMapping("/alumno/historial")
    public String alumnoHistorial(Authentication authentication, Model model) {
        Optional<Usuario> opt = usuarioActual(authentication);
        if (opt.isEmpty()) return "redirect:/login";
        Usuario usuario = opt.get();

        model.addAttribute("usuario", usuario);
        model.addAttribute("asistencias", asistenciaRepository.findByUsuarioId(usuario.getId()));
        model.addAttribute("rol", "Alumno");
        model.addAttribute("subtitulo", "Historial de asistencias");
        return "alumno/historial";
    }

    @GetMapping("/alumno/horario")
    public String alumnoHorario(Authentication authentication, Model model) {
        Optional<Usuario> opt = usuarioActual(authentication);
        if (opt.isEmpty()) return "redirect:/login";
        Usuario usuario = opt.get();

        model.addAttribute("usuario", usuario);
        model.addAttribute("horario", horarioService.obtenerHorarioDeUsuario(usuario.getId()).orElse(null));
        model.addAttribute("rol", "Alumno");
        model.addAttribute("subtitulo", "Mi horario");
        return "alumno/horario";
    }

    @GetMapping("/registro")
    public String registroForm(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "registro";
    }

    @PostMapping("/registro")
    public String registrarDesdeWeb(@Valid @ModelAttribute Usuario usuario, BindingResult bindingResult,
                                    @RequestParam String password,
                                    @RequestParam String confirmar,
                                    RedirectAttributes redirectAttributes, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("error", "Revisá los campos: nombre, apellido, email y usuario son obligatorios");
            return "registro";
        }
        if (usuario.getUsername() != null) usuario.setUsername(usuario.getUsername().trim());
        if (usuario.getEmail() != null) usuario.setEmail(usuario.getEmail().trim());
        if (esBlanco(usuario.getUsername()) || esBlanco(usuario.getEmail()) || esBlanco(password)) {
            redirectAttributes.addFlashAttribute("error", "Completá todos los campos obligatorios");
            return "redirect:/registro";
        }
        if (!password.equals(confirmar)) {
            redirectAttributes.addFlashAttribute("error", "Las contraseñas no coinciden");
            return "redirect:/registro";
        }
        if (password.trim().length() < 6) {
            redirectAttributes.addFlashAttribute("error", "La contraseña debe tener al menos 6 caracteres");
            return "redirect:/registro";
        }
        if (usuarioRepository.findByUsernameIgnoreCase(usuario.getUsername()).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Ese nombre de usuario ya está en uso");
            return "redirect:/registro";
        }
        if (usuarioRepository.findByEmailIgnoreCase(usuario.getEmail()).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Ese email ya está registrado");
            return "redirect:/registro";
        }
        usuarioService.registrarUsuarioPublico(usuario, password);
        redirectAttributes.addFlashAttribute("mensaje", "¡Cuenta creada con éxito! Ahora iniciá sesión");
        return "redirect:/login";
    }

    private Optional<Usuario> usuarioActual(Authentication authentication) {
        if (authentication == null) return Optional.empty();
        return usuarioRepository.findByUsernameIgnoreCase(authentication.getName());
    }

    private boolean esBlanco(String s) {
        return s == null || s.isBlank();
    }
}
