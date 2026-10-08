package dev.barboza.pulso.api;

import java.util.stream.Collectors;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import dev.barboza.pulso.seguranca.Excecoes;
import dev.barboza.pulso.servico.Erros;

/** Todos os erros da API saem como application/problem+json (RFC 9457), em português. */
@RestControllerAdvice
public class TratadorDeErros {

    @ExceptionHandler(Erros.NaoEncontrado.class)
    ProblemDetail naoEncontrado(Erros.NaoEncontrado e) {
        return problema(HttpStatus.NOT_FOUND, "Não encontrado", e.getMessage());
    }

    @ExceptionHandler(Erros.RegraDeNegocio.class)
    ProblemDetail regra(Erros.RegraDeNegocio e) {
        return problema(HttpStatus.UNPROCESSABLE_CONTENT, "Operação não permitida", e.getMessage());
    }

    @ExceptionHandler(Excecoes.CredenciaisInvalidas.class)
    ProblemDetail credenciais(Excecoes.CredenciaisInvalidas e) {
        return problema(HttpStatus.UNAUTHORIZED, "Não foi possível entrar", e.getMessage());
    }

    @ExceptionHandler(Excecoes.UsuarioBloqueado.class)
    ProblemDetail bloqueado(Excecoes.UsuarioBloqueado e) {
        return problema(HttpStatus.TOO_MANY_REQUESTS, "Conta bloqueada por alguns minutos", e.getMessage());
    }

    @ExceptionHandler(Excecoes.AcessoNegado.class)
    ProblemDetail negado(Excecoes.AcessoNegado e) {
        return problema(HttpStatus.FORBIDDEN, "Acesso negado", e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalido(MethodArgumentNotValidException e) {
        String detalhe = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage()).collect(Collectors.joining("; "));
        return problema(HttpStatus.BAD_REQUEST, "Dados inválidos", detalhe);
    }

    @ExceptionHandler({ HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class })
    ProblemDetail ilegivel(Exception e) {
        return problema(HttpStatus.BAD_REQUEST, "Requisição ilegível", "Confira o formato dos campos enviados.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integridade(DataIntegrityViolationException e) {
        return problema(HttpStatus.CONFLICT, "Conflito de dados", "Esse registro já existe ou está em uso.");
    }

    private static ProblemDetail problema(HttpStatus status, String titulo, String detalhe) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(status, detalhe);
        p.setTitle(titulo);
        return p;
    }
}
