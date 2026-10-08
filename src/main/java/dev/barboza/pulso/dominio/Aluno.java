package dev.barboza.pulso.dominio;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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

@Entity
@Table(name = "aluno")
public class Aluno extends Auditavel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, unique = true, length = 14)
    private String cpf;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false)
    private LocalDate nascimento;

    @Embedded
    private Endereco endereco;

    /** Unidade de origem (onde o aluno se matriculou). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id")
    private Unidade unidade;

    @OneToMany(mappedBy = "aluno")
    @OrderBy("inicio DESC, id DESC")
    private List<Matricula> matriculas = new ArrayList<>();

    @OneToMany(mappedBy = "aluno", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("data DESC")
    private List<AvaliacaoFisica> avaliacoes = new ArrayList<>();

    protected Aluno() {
    }

    public Aluno(String nome, String cpf, String email, LocalDate nascimento, Endereco endereco, Unidade unidade) {
        this.nome = nome;
        this.cpf = cpf;
        this.email = email;
        this.nascimento = nascimento;
        this.endereco = endereco;
        this.unidade = unidade;
    }

    public void atualizar(String nome, String email, LocalDate nascimento, Endereco endereco) {
        this.nome = nome;
        this.email = email;
        this.nascimento = nascimento;
        this.endereco = endereco;
    }

    /** A matrícula que vale hoje (ATIVA ou TRANCADA), se houver. */
    public Optional<Matricula> matriculaVigente() {
        return matriculas.stream().filter(Matricula::vigente).findFirst();
    }

    public int idade(LocalDate hoje) {
        return java.time.Period.between(nascimento, hoje).getYears();
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getCpf() { return cpf; }
    public String getEmail() { return email; }
    public LocalDate getNascimento() { return nascimento; }
    public Endereco getEndereco() { return endereco; }
    public Unidade getUnidade() { return unidade; }
    public List<Matricula> getMatriculas() { return matriculas; }
    public List<AvaliacaoFisica> getAvaliacoes() { return avaliacoes; }
}
