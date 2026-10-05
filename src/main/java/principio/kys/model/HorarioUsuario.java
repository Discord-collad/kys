package principio.kys.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "horario_usuario")
@IdClass(HorarioUsuario.HorarioUsuarioId.class)
public class HorarioUsuario {
    @Id
    @Column(name = "usuario_id")
    private Integer usuarioId;

    @Id
    @Column(name = "horario_id")
    private Integer horarioId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", insertable = false, updatable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "horario_id", insertable = false, updatable = false)
    private Horario horario;

    public Integer getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Integer usuarioId) { this.usuarioId = usuarioId; }
    public Integer getHorarioId() { return horarioId; }
    public void setHorarioId(Integer horarioId) { this.horarioId = horarioId; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public Horario getHorario() { return horario; }
    public void setHorario(Horario horario) { this.horario = horario; }

    public static class HorarioUsuarioId implements Serializable {
        private Integer usuarioId;
        private Integer horarioId;

        public HorarioUsuarioId() {}
        public HorarioUsuarioId(Integer usuarioId, Integer horarioId) {
            this.usuarioId = usuarioId;
            this.horarioId = horarioId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof HorarioUsuarioId other)) return false;
            return Objects.equals(usuarioId, other.usuarioId) && Objects.equals(horarioId, other.horarioId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(usuarioId, horarioId);
        }
    }
}
