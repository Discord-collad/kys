package principio.kys.service;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import principio.kys.model.Asistencia;
import principio.kys.model.Usuario;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * [P5] Exportación CSV de asistencias (solo datos reales, sin dependencias externas).
 * Separador ';' y BOM UTF-8 para compatibilidad con Excel en configuración regional es-AR/es.
 */
@Service
public class ReporteCsvService {

    private static final DateTimeFormatter F_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter F_HORA = DateTimeFormatter.ofPattern("HH:mm");

    public String asistenciasCsv(List<Asistencia> asistencias) {
        StringBuilder sb = new StringBuilder();
        sb.append("Usuario;Email;Fecha;Entrada;Salida;Estado;Observaciones;Validado por;Hora validacion\r\n");
        for (Asistencia a : asistencias) {
            Usuario u = a.getUsuario();
            sb.append(esc(u != null ? nombreCompleto(u) : ""));
            sb.append(';').append(esc(u != null && u.getEmail() != null ? u.getEmail() : ""));
            sb.append(';').append(a.getFecha() != null ? a.getFecha().format(F_FECHA) : "");
            sb.append(';').append(a.getHoraEntrada() != null ? a.getHoraEntrada().format(F_HORA) : "");
            sb.append(';').append(a.getHoraSalida() != null ? a.getHoraSalida().format(F_HORA) : "");
            sb.append(';').append(a.getEstadoAsistencia() != null ? a.getEstadoAsistencia() : "");
            sb.append(';').append(esc(a.getObservaciones() != null ? a.getObservaciones() : ""));
            sb.append(';').append(esc(a.getValidadoPor() != null ? nombreCompleto(a.getValidadoPor()) : ""));
            sb.append(';').append(a.getHoraValidacion() != null ? a.getHoraValidacion().format(F_HORA) : "");
            sb.append("\r\n");
        }
        return sb.toString();
    }

    /** Respuesta HTTP estándar para descargar el CSV como archivo. */
    @SuppressWarnings("null")
    public static ResponseEntity<byte[]> respuesta(String csv, String nombreArchivo) {
        Charset utf8 = StandardCharsets.UTF_8;
        byte[] bytes = ("\uFEFF" + csv).getBytes(utf8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", utf8));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"");
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    private String nombreCompleto(Usuario u) {
        String n = u.getNombre() != null ? u.getNombre() : "";
        String ap = u.getApellido() != null ? u.getApellido() : "";
        return (n + " " + ap).trim();
    }

    private String esc(String valor) {
        if (valor == null) return "";
        if (valor.contains(";") || valor.contains("\"") || valor.contains("\n") || valor.contains("\r")) {
            return '"' + valor.replace("\"", "\"\"") + '"';
        }
        return valor;
    }
}
