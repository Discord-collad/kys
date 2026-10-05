package principio.kys.controller;

import principio.kys.model.Asistencia;
import principio.kys.service.AsistenciaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/asistencias")
public class AsistenciaController {
    private final AsistenciaService asistenciaService;

    @Autowired
    public AsistenciaController(AsistenciaService asistenciaService) {
        this.asistenciaService = asistenciaService;
    }

    @PostMapping
    public ResponseEntity<?> registrar(@RequestBody Map<String, Integer> body) {
        Integer usuarioId = body.get("usuarioId");
        if (usuarioId == null) {
            return ResponseEntity.badRequest().body("Falta usuarioId");
        }
        // [P1] Doble entrada ahora responde 409 (antes devolvía 200 con la asistencia existente)
        if (asistenciaService.obtenerDeHoy(usuarioId).isPresent()) {
            return ResponseEntity.status(409).body("El usuario ya registró entrada hoy");
        }
        return asistenciaService.buscarUsuarioParaEntrada(usuarioId)
                .<ResponseEntity<?>>map(usuario -> {
                    try {
                        return ResponseEntity.ok(asistenciaService.registrarEntrada(usuario));
                    } catch (org.springframework.dao.DataIntegrityViolationException e) {
                        // Carrera: otro request ganó el insert del unique (usuario_id, fecha)
                        return ResponseEntity.status(409).body("El usuario ya registró entrada hoy");
                    }
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/usuario/{userId}")
    public List<Asistencia> porUsuario(@PathVariable Integer userId) {
        return asistenciaService.listarPorUsuario(userId);
    }

    @GetMapping("/fechas")
    public List<Asistencia> porFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return asistenciaService.listarPorFechas(desde, hasta);
    }
}
