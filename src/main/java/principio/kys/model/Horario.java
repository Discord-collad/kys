package principio.kys.model;

import jakarta.persistence.*;

@Entity
@Table(name = "horarios")
public class Horario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 80)
    private String nombre;

    @Column(name = "hora_entrada", nullable = false)
    private java.time.LocalTime horaEntrada;

    @Column(name = "hora_salida", nullable = false)
    private java.time.LocalTime horaSalida;

    @Column(nullable = false)
    private Integer toleranciaMinutos = 15;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public java.time.LocalTime getHoraEntrada() { return horaEntrada; }
    public void setHoraEntrada(java.time.LocalTime horaEntrada) { this.horaEntrada = horaEntrada; }
    public java.time.LocalTime getHoraSalida() { return horaSalida; }
    public void setHoraSalida(java.time.LocalTime horaSalida) { this.horaSalida = horaSalida; }
    public Integer getToleranciaMinutos() { return toleranciaMinutos; }
    public void setToleranciaMinutos(Integer toleranciaMinutos) { this.toleranciaMinutos = toleranciaMinutos; }
}
