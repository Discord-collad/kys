package principio.kys.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "configuraciones_examenes")
public class ConfiguracionExamen {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Boolean semanaExamenesActiva;

    @Column
    private LocalDate fechaInicioSemana;

    @Column
    private LocalDate fechaFinSemana;

    @Column(length = 200)
    private String mensajePersonalizado;

    // Nombres explícitos: la estrategia implícita de Hibernate mapea "mostrarAAumnos"
    // (doble mayúscula) a una columna incorrecta contra el DDL real.
    @Column(name = "mostrar_a_alumnos", nullable = false)
    private Boolean mostrarAAumnos;

    @Column(name = "mostrar_a_profesores", nullable = false)
    private Boolean mostrarAProfesores;

    public ConfiguracionExamen() {
        this.semanaExamenesActiva = false;
        this.mostrarAAumnos = true;
        this.mostrarAProfesores = true;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Boolean getSemanaExamenesActiva() { return semanaExamenesActiva; }
    public void setSemanaExamenesActiva(Boolean semanaExamenesActiva) { this.semanaExamenesActiva = semanaExamenesActiva; }

    public LocalDate getFechaInicioSemana() { return fechaInicioSemana; }
    public void setFechaInicioSemana(LocalDate fechaInicioSemana) { this.fechaInicioSemana = fechaInicioSemana; }

    public LocalDate getFechaFinSemana() { return fechaFinSemana; }
    public void setFechaFinSemana(LocalDate fechaFinSemana) { this.fechaFinSemana = fechaFinSemana; }

    public String getMensajePersonalizado() { return mensajePersonalizado; }
    public void setMensajePersonalizado(String mensajePersonalizado) { this.mensajePersonalizado = mensajePersonalizado; }

    public Boolean getMostrarAAumnos() { return mostrarAAumnos; }
    public void setMostrarAAumnos(Boolean mostrarAAumnos) { this.mostrarAAumnos = mostrarAAumnos; }

    public Boolean getMostrarAProfesores() { return mostrarAProfesores; }
    public void setMostrarAProfesores(Boolean mostrarAProfesores) { this.mostrarAProfesores = mostrarAProfesores; }
}
