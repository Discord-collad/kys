package principio.kys.controller;

import principio.kys.model.ConfiguracionExamen;
import principio.kys.model.PeriodoLectivo;
import principio.kys.service.ConfiguracionExamenService;
import principio.kys.service.PeriodoLectivoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * [Rol DIRECTOR] Configuracion del sistema: periodos lectivos y semana de examenes.
 * POST -> RedirectAttributes (flash), nunca model directo tras redirect.
 */
@Controller
@RequestMapping("/admin/configuracion")
@PreAuthorize("hasRole('DIRECTOR')")
public class ConfiguracionController {
    private final PeriodoLectivoService periodoService;
    private final ConfiguracionExamenService examenService;

    public ConfiguracionController(PeriodoLectivoService periodoService,
                                    ConfiguracionExamenService examenService) {
        this.periodoService = periodoService;
        this.examenService = examenService;
    }

    @GetMapping
    public String configuracion(Model model) {
        model.addAttribute("periodos", periodoService.listarTodos());
        model.addAttribute("periodoActivo", periodoService.obtenerPeriodoActivo().orElse(null));
        model.addAttribute("ultimoPeriodo", periodoService.obtenerUltimoPeriodo().orElse(null));
        model.addAttribute("configExamen", examenService.obtenerOCrearConfiguracion());
        model.addAttribute("nuevoPeriodo", new PeriodoLectivo());
        model.addAttribute("rol", "Dirección");
        model.addAttribute("subtitulo", "Configuración del Sistema");
        return "director/configuracion";
    }

    @PostMapping("/periodos/guardar")
    public String guardarPeriodo(@ModelAttribute PeriodoLectivo periodo, RedirectAttributes ra) {
        try {
            periodoService.guardar(periodo);
            ra.addFlashAttribute("mensaje", "Periodo guardado correctamente");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/configuracion";
    }

    @PostMapping("/periodos/activar/{id}")
    public String activarPeriodo(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            periodoService.activarPeriodo(id);
            ra.addFlashAttribute("mensaje", "Periodo activado correctamente");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/configuracion";
    }

    @PostMapping("/periodos/marcar-ultimo/{id}")
    public String marcarUltimoPeriodo(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            periodoService.marcarComoUltimoPeriodo(id);
            ra.addFlashAttribute("mensaje", "Periodo marcado como último correctamente");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/configuracion";
    }

    @PostMapping("/periodos/eliminar/{id}")
    public String eliminarPeriodo(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            periodoService.eliminar(id);
            ra.addFlashAttribute("mensaje", "Periodo eliminado correctamente");
        } catch (IllegalArgumentException e) {
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se pudo eliminar el periodo");
        }
        return "redirect:/admin/configuracion";
    }

    /**
     * Guarda fechas/mensaje/visibilidad de la semana de examenes SIN tocar el flag
     * activo (activar/desactivar son acciones separadas).
     */
    @PostMapping("/examenes/guardar")
    public String guardarConfigExamen(@ModelAttribute ConfiguracionExamen config, RedirectAttributes ra) {
        ConfiguracionExamen actual = examenService.obtenerOCrearConfiguracion();
        if (config.getFechaInicioSemana() != null && config.getFechaFinSemana() != null
                && config.getFechaFinSemana().isBefore(config.getFechaInicioSemana())) {
            ra.addFlashAttribute("error", "La fecha fin de la semana debe ser posterior o igual a la fecha inicio");
            return "redirect:/admin/configuracion";
        }
        if (config.getMensajePersonalizado() != null && config.getMensajePersonalizado().length() > 200) {
            ra.addFlashAttribute("error", "El mensaje personalizado no puede superar 200 caracteres");
            return "redirect:/admin/configuracion";
        }
        actual.setFechaInicioSemana(config.getFechaInicioSemana());
        actual.setFechaFinSemana(config.getFechaFinSemana());
        actual.setMensajePersonalizado(config.getMensajePersonalizado());
        actual.setMostrarAAumnos(Boolean.TRUE.equals(config.getMostrarAAumnos()));
        actual.setMostrarAProfesores(Boolean.TRUE.equals(config.getMostrarAProfesores()));
        examenService.guardar(actual);
        ra.addFlashAttribute("mensaje", "Configuración de exámenes actualizada");
        return "redirect:/admin/configuracion";
    }

    @PostMapping("/examenes/activar")
    public String activarSemanaExamenes(RedirectAttributes ra) {
        examenService.activarSemanaExamenes();
        ra.addFlashAttribute("mensaje", "Semana de exámenes activada");
        return "redirect:/admin/configuracion";
    }

    @PostMapping("/examenes/desactivar")
    public String desactivarSemanaExamenes(RedirectAttributes ra) {
        examenService.desactivarSemanaExamenes();
        ra.addFlashAttribute("mensaje", "Semana de exámenes desactivada");
        return "redirect:/admin/configuracion";
    }
}
