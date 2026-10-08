package dev.barboza.pulso.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;

import dev.barboza.pulso.dominio.CanalDeContato;
import dev.barboza.pulso.dominio.ResultadoDoContato;
import dev.barboza.pulso.servico.MatriculaService.Forma;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Entradas da API (com Bean Validation) e o envelope de paginação. Nenhuma entidade JPA sai pelo JSON. */
public final class Dtos {

    private Dtos() {
    }

    public record AlunoEntrada(
            @NotBlank @Size(max = 120) String nome,
            @NotBlank String cpf,
            @NotBlank @Email @Size(max = 120) String email,
            @NotNull @Past LocalDate nascimento,
            @NotBlank @Size(max = 80) String bairro,
            @NotBlank @Size(max = 60) String cidade,
            @NotBlank @Pattern(regexp = "[A-Za-z]{2}", message = "use a sigla da UF") String uf,
            @NotNull Long unidadeId) {
    }

    public record AvaliacaoEntrada(
            @NotNull @PastOrPresent LocalDate data,
            @NotNull @DecimalMin("20.0") @DecimalMax("400.0") BigDecimal peso,
            @NotNull @DecimalMin("0.50") @DecimalMax("2.50") BigDecimal altura,
            @DecimalMin("2.0") @DecimalMax("70.0") BigDecimal percentualGordura,
            @DecimalMin("30.0") @DecimalMax("250.0") BigDecimal cinturaCm) {
    }

    public record ContatoEntrada(@NotNull CanalDeContato canal, @NotNull ResultadoDoContato resultado,
            @Size(max = 200) String observacao) {
    }

    public record MatriculaEntrada(@NotNull Long alunoId, @NotNull Long planoId) {
    }

    public record CancelamentoEntrada(@Size(max = 80) String motivo) {
    }

    public record TrocaDePlanoEntrada(@NotNull Long planoId) {
    }

    public record PagamentoEntrada(@NotNull Forma forma, @Size(max = 60) String referencia) {
    }

    public record EntradaNaCatraca(String cpf, Long alunoId, @NotNull Long unidadeId) {
    }

    public record SaidaDaCatraca(@NotNull Long alunoId) {
    }

    public record ReservaEntrada(@NotNull Long alunoId) {
    }

    public record Pagina<T>(List<T> conteudo, int pagina, int tamanho, long total, int paginas) {
        public static <T> Pagina<T> de(Page<T> p) {
            return new Pagina<>(p.getContent(), p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
        }
    }
}
