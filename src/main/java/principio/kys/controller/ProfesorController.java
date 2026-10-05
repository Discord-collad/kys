package principio.kys.controller;

import principio.kys.model.Asistencia;
import principio.kys.model.Usuario;
import principio.kys.service.AsistenciaService;
import principio.kys.service.UsuarioService;
import principio.kys.service.ConfiguracionExamenService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/profesor")
@PreAuthorize("hasAnyRole('DIRECTOR', 'PROFESOR')")
public class ProfesorController {
    private final AsistenciaService asistenciaService;
    private final UsuarioService usuarioService;
    private final ConfiguracionExamenService examenService;

    private final principio.kys.service.ReporteCsvService reporteCsvService;

    public ProfesorController(AsistenciaService asistenciaService, UsuarioService usuarioService,
                              ConfiguracionExamenService examenService,
                              principio.kys.service.ReporteCsvService reporteCsvService) {
        this.asistenciaService = asistenciaService;
        this.usuarioService = usuarioService;
        this.examenService = examenService;
        this.reporteCsvService = reporteCsvService;
    }

    @GetMapping
    public String panel(Model model) {
        List<Asistencia> pendientes = asistenciaService.listarPorEstado("PENDIENTE");
        model.addAttribute("pendientes", pendientes);
        model.addAttribute("hoy", asistenciaService.listarDeHoy());
        model.addAttribute("configExamen", examenService.obtenerConfiguracionActual().orElse(null));
        model.addAttribute("mostrarAvisoExamenes", examenService.debeMostrarAProfesores());
        model.addAttribute("rol", "Profesor");
        model.addAttribute("subtitulo", "Validación de asistencia");
        return "profesor/panel";
    }

    @PostMapping("/validar")
    @SuppressWarnings("null")
    public String validar(@RequestParam @NonNull Integer asistenciaId,
                          @RequestParam boolean aprobado,
                          @RequestParam(required = false) String observacion,
                          Authentication auth,
                          RedirectAttributes ra) {
        Optional<Usuario> opt = auth == null ? Optional.empty()
                : usuarioService.buscarPorIdOpt(usuarioService.buscarPorUsername(auth.getName()).getId());
        if (opt.isEmpty()) {
            ra.addFlashAttribute("error", "No se pudo identificar al profesor");
            return "redirect:/profesor";
        }
        try {
            asistenciaService.validar(asistenciaId, opt.get(), aprobado, observacion);
            ra.addFlashAttribute("mensaje", aprobado ? "Asistencia aprobada" : "Asistencia rechazada");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se pudo validar la asistencia");
        }
        return "redirect:/profesor";
    }

    @GetMapping("/asistencias")
    public String verAsistencias(@RequestParam(required = false) String desde,
                                 @RequestParam(required = false) String hasta,
                                 Model model) {
        LocalDate hoy = LocalDate.now();
        LocalDate inicio;
        LocalDate fin;
        try {
            inicio = (desde == null || desde.isBlank()) ? hoy.minusDays(7) : LocalDate.parse(desde);
            fin = (hasta == null || hasta.isBlank()) ? hoy : LocalDate.parse(hasta);
        } catch (java.time.format.DateTimeParseException e) {
            model.addAttribute("error", "Formato de fecha inválido. Usá YYYY-MM-DD.");
            inicio = hoy.minusDays(7);
            fin = hoy;
        }
        if (inicio.isAfter(fin)) {
            model.addAttribute("error", "El rango es inválido: 'desde' es posterior a 'hasta'. Se muestra la última semana.");
            inicio = hoy.minusDays(7);
            fin = hoy;
        }
        model.addAttribute("asistencias", asistenciaService.listarPorFechas(inicio, fin));
        model.addAttribute("desde", inicio);
        model.addAttribute("hasta", fin);
        model.addAttribute("rol", "Profesor");
        model.addAttribute("subtitulo", "Historial de asistencias");
        return "profesor/asistencias";
    }

    /** [P5] Exportación CSV del rango consultado (mismo rango por defecto que la vista). */
    @GetMapping("/asistencias/csv")
    public org.springframework.http.ResponseEntity<byte[]> exportarCsv(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta) {
        LocalDate hoy = LocalDate.now();
        LocalDate inicio;
        LocalDate fin;
        try {
            inicio = (desde == null || desde.isBlank()) ? hoy.minusDays(7) : LocalDate.parse(desde);
            fin = (hasta == null || hasta.isBlank()) ? hoy : LocalDate.parse(hasta);
        } catch (java.time.format.DateTimeParseException e) {
            inicio = hoy.minusDays(7);
            fin = hoy;
        }
        if (inicio.isAfter(fin)) { LocalDate tmp = inicio; inicio = fin; fin = tmp; }
        String csv = reporteCsvService.asistenciasCsv(asistenciaService.listarPorFechas(inicio, fin));
        String nombre = "asistencias_" + inicio + "_a_" + fin + ".csv";
        return principio.kys.service.ReporteCsvService.respuesta(csv, nombre);
    }

    @PostMapping("/examenes/activar")
    public String activarSemanaExamenes(RedirectAttributes ra) {
        examenService.activarSemanaExamenes();
        ra.addFlashAttribute("mensaje", "Semana de exámenes activada");
        return "redirect:/profesor";
    }

    @PostMapping("/examenes/desactivar")
    public String desactivarSemanaExamenes(RedirectAttributes ra) {
        examenService.desactivarSemanaExamenes();
        ra.addFlashAttribute("mensaje", "Semana de exámenes desactivada");
        return "redirect:/profesor";
    }
}
