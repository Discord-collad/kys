package principio.kys.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "periodos_lectivos")
public class PeriodoLectivo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Column(nullable = false)
    private LocalDate fechaInicio;

    @Column(nullable = false)
    private LocalDate fechaFin;

    @Column(nullable = false)
    private Integer duracionSemanas;

    @Column(nullable = false)
    private Boolean activo;

    @Column(nullable = false)
    private Boolean esUltimoPeriodo;

    public PeriodoLectivo() {
        this.activo = true;
        this.esUltimoPeriodo = false;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public Integer getDuracionSemanas() { return duracionSemanas; }
    public void setDuracionSemanas(Integer duracionSemanas) { this.duracionSemanas = duracionSemanas; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }

    public Boolean getEsUltimoPeriodo() { return esUltimoPeriodo; }
    public void setEsUltimoPeriodo(Boolean esUltimoPeriodo) { this.esUltimoPeriodo = esUltimoPeriodo; }
}
