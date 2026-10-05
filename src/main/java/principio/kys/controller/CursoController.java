package principio.kys.controller;

import principio.kys.model.Curso;
import principio.kys.repository.UsuarioRepository;
import principio.kys.service.CursoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/cursos")
@PreAuthorize("hasRole('DIRECTOR')")
public class CursoController {
    private final CursoService cursoService;
    private final UsuarioRepository usuarioRepository;

    public CursoController(CursoService cursoService, UsuarioRepository usuarioRepository) {
        this.cursoService = cursoService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("cursos", cursoService.listarTodos());
        model.addAttribute("rol", "Dirección");
        model.addAttribute("subtitulo", "Gestión de Cursos");
        return "director/cursos";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("curso", new Curso());
        model.addAttribute("rol", "Dirección");
        model.addAttribute("subtitulo", "Nuevo Curso");
        return "director/curso-form";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Curso curso, RedirectAttributes ra) {
        cursoService.guardar(curso);
        ra.addFlashAttribute("mensaje", "Curso guardado");
        return "redirect:/admin/cursos";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Integer id, Model model) {
        model.addAttribute("curso", cursoService.buscarPorId(id).orElse(new Curso()));
        model.addAttribute("rol", "Dirección");
        model.addAttribute("subtitulo", "Editar Curso");
        return "director/curso-form";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Integer id, RedirectAttributes ra) {
        try {
            cursoService.eliminar(id);
            ra.addFlashAttribute("mensaje", "Curso eliminado");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "No se pudo eliminar el curso");
        }
        return "redirect:/admin/cursos";
    }

    @GetMapping("/asignar")
    public String asignarForm(Model model) {
        model.addAttribute("cursos", cursoService.listarTodos());
        model.addAttribute("alumnos", usuarioRepository.findAll());
        model.addAttribute("rol", "Dirección");
        model.addAttribute("subtitulo", "Asignar Curso");
        return "director/curso-asignar";
    }

    @PostMapping("/asignar")
    public String asignar(@RequestParam Integer cursoId,
                          @RequestParam(required = false) List<Integer> usuarioIds,
                          RedirectAttributes ra) {
        cursoService.asignarUsuariosACurso(cursoId, usuarioIds);
        ra.addFlashAttribute("mensaje", "Alumnos asignados al curso");
        return "redirect:/admin/cursos/asignar";
    }
}
