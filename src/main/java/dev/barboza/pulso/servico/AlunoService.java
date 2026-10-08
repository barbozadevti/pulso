package dev.barboza.pulso.servico;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.barboza.pulso.dominio.Aluno;
import dev.barboza.pulso.dominio.AvaliacaoFisica;
import dev.barboza.pulso.dominio.Endereco;
import dev.barboza.pulso.dominio.StatusMatricula;
import dev.barboza.pulso.dominio.Unidade;
import dev.barboza.pulso.repositorio.AlunoRepository;
import dev.barboza.pulso.repositorio.AvaliacaoFisicaRepository;
import dev.barboza.pulso.repositorio.UnidadeRepository;

@Service
@Transactional
public class AlunoService {

    public record DadosDoAluno(String nome, String cpf, String email, LocalDate nascimento, String bairro,
            String cidade, String uf, Long unidadeId) {
    }

    private final AlunoRepository alunos;
    private final UnidadeRepository unidades;
    private final AvaliacaoFisicaRepository avaliacoes;
    private final Clock relogio;

    public AlunoService(AlunoRepository alunos, UnidadeRepository unidades, AvaliacaoFisicaRepository avaliacoes,
            Clock relogio) {
        this.alunos = alunos;
        this.unidades = unidades;
        this.avaliacoes = avaliacoes;
        this.relogio = relogio;
    }

    public Aluno criar(DadosDoAluno d) {
        if (!Cpf.valido(d.cpf())) {
            throw new Erros.RegraDeNegocio("CPF inválido.");
        }
        String cpf = Cpf.formatar(d.cpf());
        if (alunos.existsByCpf(cpf)) {
            throw new Erros.RegraDeNegocio("Já existe um aluno com o CPF " + cpf + ".");
        }
        if (alunos.existsByEmailIgnoreCase(d.email())) {
            throw new Erros.RegraDeNegocio("Já existe um aluno com o e-mail " + d.email() + ".");
        }
        Unidade unidade = unidades.findById(d.unidadeId())
                .orElseThrow(() -> new Erros.NaoEncontrado("Unidade " + d.unidadeId() + " não encontrada."));
        return alunos.save(new Aluno(d.nome().trim(), cpf, d.email().trim().toLowerCase(), d.nascimento(),
                new Endereco(d.bairro().trim(), d.cidade().trim(), d.uf().trim().toUpperCase()), unidade));
    }

    public Aluno atualizar(Long id, DadosDoAluno d) {
        Aluno a = buscar(id);
        a.atualizar(d.nome().trim(), d.email().trim().toLowerCase(), d.nascimento(),
                new Endereco(d.bairro().trim(), d.cidade().trim(), d.uf().trim().toUpperCase()));
        return a;
    }

    @Transactional(readOnly = true)
    public Aluno buscar(Long id) {
        return alunos.buscarComUnidade(id).orElseThrow(() -> new Erros.NaoEncontrado("Aluno " + id + " não encontrado."));
    }

    @Transactional(readOnly = true)
    public Page<Aluno> listar(String busca, Long unidadeId, String bairro, StatusMatricula situacao,
            Pageable pagina) {
        Specification<Aluno> filtro = Specification.where(AlunoSpecs.comUnidade()).and(AlunoSpecs.nomeOuCpfContem(busca))
                .and(AlunoSpecs.daUnidade(unidadeId)).and(AlunoSpecs.doBairro(bairro))
                .and(AlunoSpecs.comSituacao(situacao));
        return alunos.findAll(filtro, pagina);
    }

    // ---- avaliações físicas

    public AvaliacaoFisica registrarAvaliacao(Long alunoId, LocalDate data, BigDecimal peso, BigDecimal altura,
            BigDecimal gordura, BigDecimal cintura) {
        if (data.isAfter(LocalDate.now(relogio))) {
            throw new Erros.RegraDeNegocio("A data da avaliação não pode ser futura.");
        }
        return avaliacoes.save(new AvaliacaoFisica(buscar(alunoId), data, peso, altura, gordura, cintura));
    }

    @Transactional(readOnly = true)
    public List<AvaliacaoFisica> avaliacoesDoAluno(Long alunoId) {
        buscar(alunoId);
        return avaliacoes.findByAlunoIdOrderByDataAsc(alunoId);
    }

    public AvaliacaoFisica atualizarAvaliacao(Long id, LocalDate data, BigDecimal peso, BigDecimal altura,
            BigDecimal gordura, BigDecimal cintura) {
        AvaliacaoFisica a = avaliacoes.findById(id)
                .orElseThrow(() -> new Erros.NaoEncontrado("Avaliação " + id + " não encontrada."));
        a.atualizar(data, peso, altura, gordura, cintura);
        return a;
    }

    public void removerAvaliacao(Long id) {
        AvaliacaoFisica a = avaliacoes.findById(id)
                .orElseThrow(() -> new Erros.NaoEncontrado("Avaliação " + id + " não encontrada."));
        avaliacoes.delete(a);
    }
}
