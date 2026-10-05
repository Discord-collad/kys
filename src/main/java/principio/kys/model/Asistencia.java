package principio.kys.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "asistencias", uniqueConstraints = @UniqueConstraint(
        name = "ux_asistencias_usuario_fecha", columnNames = {"usuario_id", "fecha"}))
public class Asistencia {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    private LocalDate fecha;
    private LocalTime horaEntrada;
    private LocalTime horaSalida;

    @Column(length = 20)
    private String estadoAsistencia = "PENDIENTE";

    @Column(length = 300)
    private String observaciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "validado_por_id")
    private Usuario validadoPor;

    private LocalTime horaValidacion;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }
    public LocalTime getHoraEntrada() { return horaEntrada; }
    public void setHoraEntrada(LocalTime horaEntrada) { this.horaEntrada = horaEntrada; }
    public LocalTime getHoraSalida() { return horaSalida; }
    public void setHoraSalida(LocalTime horaSalida) { this.horaSalida = horaSalida; }
    public String getEstadoAsistencia() { return estadoAsistencia; }
    public void setEstadoAsistencia(String estadoAsistencia) { this.estadoAsistencia = estadoAsistencia; }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }
    public Usuario getValidadoPor() { return validadoPor; }
    public void setValidadoPor(Usuario validadoPor) { this.validadoPor = validadoPor; }
    public LocalTime getHoraValidacion() { return horaValidacion; }
    public void setHoraValidacion(LocalTime horaValidacion) { this.horaValidacion = horaValidacion; }
}
