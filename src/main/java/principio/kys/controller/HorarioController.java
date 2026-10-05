package principio.kys.controller;

import principio.kys.model.Horario;
import principio.kys.repository.UsuarioRepository;
import principio.kys.service.HorarioService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/horarios")
@PreAuthorize("hasRole('DIRECTOR')")
public class HorarioController {
    private final HorarioService horarioService;
    private final UsuarioRepository usuarioRepository;

    public HorarioController(HorarioService horarioService, UsuarioRepository usuarioRepository) {
        this.horarioService = horarioService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("horarios", horarioService.listarTodos());
        model.addAttribute("rol", "Dirección");
        model.addAttribute("subtitulo", "Gestión de Horarios");
        return "director/horarios";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("horario", new Horario());
        model.addAttribute("rol", "Dirección");
        model.addAttribute("subtitulo", "Nuevo Horario");
        return "director/horario-form";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Horario horario, RedirectAttributes ra) {
        if (horario.getToleranciaMinutos() == null) horario.setToleranciaMinutos(15);
        horarioService.guardar(horario);
        ra.addFlashAttribute("mensaje", "Horario guardado");
        return "redirect:/admin/horarios";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable @NonNull Integer id, Model model) {
        model.addAttribute("horario", horarioService.buscarPorId(id).orElse(new Horario()));
        model.addAttribute("rol", "Dirección");
        model.addAttribute("subtitulo", "Editar Horario");
        return "director/horario-form";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable @NonNull Integer id, RedirectAttributes ra) {
        try {
            horarioService.eliminar(id);
            ra.addFlashAttribute("mensaje", "Horario eliminado");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se pudo eliminar el horario");
        }
        return "redirect:/admin/horarios";
    }

    @GetMapping("/asignar")
    public String asignarForm(Model model) {
        model.addAttribute("usuarios", usuarioRepository.findAll());
        model.addAttribute("horarios", horarioService.listarTodos());
        model.addAttribute("rol", "Dirección");
        model.addAttribute("subtitulo", "Asignar Horario");
        return "director/horario-asignar";
    }

    @PostMapping("/asignar")
    public String asignar(@RequestParam Integer usuarioId,
                          @RequestParam(required = false) Integer horarioId,
                          RedirectAttributes ra) {
        horarioService.asignarHorarioAUsuario(usuarioId, horarioId);
        ra.addFlashAttribute("mensaje", "Horario asignado");
        return "redirect:/admin/horarios/asignar";
    }
}
