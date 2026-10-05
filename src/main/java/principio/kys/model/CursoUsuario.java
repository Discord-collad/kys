package principio.kys.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "curso_usuario")
@IdClass(CursoUsuario.CursoUsuarioId.class)
public class CursoUsuario {
    @Id
    @Column(name = "curso_id")
    private Integer cursoId;

    @Id
    @Column(name = "usuario_id")
    private Integer usuarioId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "curso_id", insertable = false, updatable = false)
    private Curso curso;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", insertable = false, updatable = false)
    private Usuario usuario;

    public Integer getCursoId() { return cursoId; }
    public void setCursoId(Integer cursoId) { this.cursoId = cursoId; }
    public Integer getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Integer usuarioId) { this.usuarioId = usuarioId; }
    public Curso getCurso() { return curso; }
    public void setCurso(Curso curso) { this.curso = curso; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }

    public static class CursoUsuarioId implements Serializable {
        private Integer cursoId;
        private Integer usuarioId;

        public CursoUsuarioId() {}
        public CursoUsuarioId(Integer cursoId, Integer usuarioId) {
            this.cursoId = cursoId;
            this.usuarioId = usuarioId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CursoUsuarioId other)) return false;
            return Objects.equals(cursoId, other.cursoId) && Objects.equals(usuarioId, other.usuarioId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(cursoId, usuarioId);
        }
    }
}
