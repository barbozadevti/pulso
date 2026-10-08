package dev.barboza.pulso.servico;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.barboza.pulso.dominio.Aluno;
import dev.barboza.pulso.dominio.CanalDeContato;
import dev.barboza.pulso.dominio.Contato;
import dev.barboza.pulso.dominio.Matricula;
import dev.barboza.pulso.dominio.ResultadoDoContato;
import dev.barboza.pulso.repositorio.AlunoRepository;
import dev.barboza.pulso.repositorio.ContatoRepository;
import dev.barboza.pulso.repositorio.MatriculaRepository;

/** Registro das abordagens de retenção. Guarda a nota de risco do momento para medir o que funciona. */
@Service
@Transactional
public class ContatoService {

    private final AlunoRepository alunos;
    private final ContatoRepository contatos;
    private final MatriculaRepository matriculas;
    private final RiscoService risco;
    private final Clock relogio;

    public ContatoService(AlunoRepository alunos, ContatoRepository contatos, MatriculaRepository matriculas,
            RiscoService risco, Clock relogio) {
        this.alunos = alunos;
        this.contatos = contatos;
        this.matriculas = matriculas;
        this.risco = risco;
        this.relogio = relogio;
    }

    public Contato registrar(Long alunoId, CanalDeContato canal, ResultadoDoContato resultado, String observacao) {
        Aluno aluno = alunos.findById(alunoId)
                .orElseThrow(() -> new Erros.NaoEncontrado("Aluno " + alunoId + " não encontrado."));
        int nota = matriculas.buscarVigente(alunoId).filter(Matricula::ativa)
                .map(m -> risco.deAluno(alunoId, m.getInicio()).pontos()).orElse(0);
        String obs = observacao == null || observacao.isBlank() ? null : observacao.trim();
        return contatos.save(new Contato(aluno, canal, resultado, obs, LocalDateTime.now(relogio), nota));
    }
}
