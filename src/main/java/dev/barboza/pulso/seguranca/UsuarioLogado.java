package dev.barboza.pulso.seguranca;

import java.io.Serializable;

/** Quem está operando. Vai na sessão HTTP; o escopo de dados (unidade ou aluno) sai daqui. */
public record UsuarioLogado(Long id, String login, String nome, Perfil perfil, Long unidadeId, Long alunoId)
        implements Serializable {

    public boolean eh(Perfil... perfis) {
        for (Perfil p : perfis) {
            if (p == perfil) {
                return true;
            }
        }
        return false;
    }
}
