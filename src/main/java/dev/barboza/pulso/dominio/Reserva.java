package dev.barboza.pulso.dominio;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "reserva")
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aula_id")
    private Aula aula;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "aluno_id")
    private Aluno aluno;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private StatusReserva status;

    @Column(name = "criada_em", nullable = false)
    private LocalDateTime criadaEm;

    protected Reserva() {
    }

    Reserva(Aula aula, Aluno aluno, StatusReserva status, LocalDateTime criadaEm) {
        this.aula = aula;
        this.aluno = aluno;
        this.status = status;
        this.criadaEm = criadaEm;
    }

    void cancelar() {
        this.status = StatusReserva.CANCELADA;
    }

    void confirmar() {
        this.status = StatusReserva.CONFIRMADA;
    }

    public Long getId() { return id; }
    public Aula getAula() { return aula; }
    public Aluno getAluno() { return aluno; }
    public StatusReserva getStatus() { return status; }
    public LocalDateTime getCriadaEm() { return criadaEm; }
}
