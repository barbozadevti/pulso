package dev.barboza.pulso.dominio;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Uma sessão de aula coletiva. O campo {@code versao} ({@code @Version}) é o bloqueio otimista:
 * duas reservas simultâneas para a última vaga não passam juntas, a segunda leva
 * OptimisticLockException e tenta de novo já enxergando a vaga ocupada.
 */
@Entity
@Table(name = "aula")
public class Aula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "modalidade_id")
    private Modalidade modalidade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id")
    private Unidade unidade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "instrutor_id")
    private Instrutor instrutor;

    @Column(nullable = false)
    private LocalDateTime inicio;

    @Column(name = "duracao_min", nullable = false)
    private int duracaoMin;

    @Column(nullable = false)
    private int vagas;

    @Version
    @Column(name = "versao", nullable = false)
    private long versao;

    @OneToMany(mappedBy = "aula", cascade = jakarta.persistence.CascadeType.ALL)
    @OrderBy("criadaEm, id")
    private List<Reserva> reservas = new ArrayList<>();

    protected Aula() {
    }

    public Aula(Modalidade modalidade, Unidade unidade, Instrutor instrutor, LocalDateTime inicio, int duracaoMin,
            int vagas) {
        this.modalidade = modalidade;
        this.unidade = unidade;
        this.instrutor = instrutor;
        this.inicio = inicio;
        this.duracaoMin = duracaoMin;
        this.vagas = vagas;
    }

    public long confirmadas() {
        return reservas.stream().filter(r -> r.getStatus() == StatusReserva.CONFIRMADA).count();
    }

    public long naEspera() {
        return reservas.stream().filter(r -> r.getStatus() == StatusReserva.ESPERA).count();
    }

    public boolean temVaga() {
        return confirmadas() < vagas;
    }

    /**
     * Reserva: confirmada se houver vaga, senão entra no fim da fila de espera.
     * A regra mora na entidade; quem chama só decide o momento (transação) e o relógio.
     */
    public Reserva reservar(Aluno aluno, LocalDateTime agora) {
        boolean jaTem = reservas.stream().anyMatch(
                r -> r.getAluno().getId().equals(aluno.getId()) && r.getStatus() != StatusReserva.CANCELADA);
        if (jaTem) {
            throw new IllegalStateException(aluno.getNome() + " já tem reserva nesta aula.");
        }
        Reserva r = new Reserva(this, aluno, temVaga() ? StatusReserva.CONFIRMADA : StatusReserva.ESPERA, agora);
        reservas.add(r);
        return r;
    }

    /** Cancela e, se liberou uma vaga confirmada, promove o primeiro da fila de espera. Devolve quem foi promovido. */
    public Reserva cancelar(Reserva reserva) {
        boolean liberouVaga = reserva.getStatus() == StatusReserva.CONFIRMADA;
        reserva.cancelar();
        if (!liberouVaga) {
            return null;
        }
        return reservas.stream().filter(r -> r.getStatus() == StatusReserva.ESPERA).findFirst().map(r -> {
            r.confirmar();
            return r;
        }).orElse(null);
    }

    public Long getId() { return id; }
    public Modalidade getModalidade() { return modalidade; }
    public Unidade getUnidade() { return unidade; }
    public Instrutor getInstrutor() { return instrutor; }
    public LocalDateTime getInicio() { return inicio; }
    public int getDuracaoMin() { return duracaoMin; }
    public int getVagas() { return vagas; }
    public long getVersao() { return versao; }
    public List<Reserva> getReservas() { return reservas; }
}
