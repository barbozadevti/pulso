package dev.barboza.pulso.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

@Configuration
public class DocumentacaoConfig {

    @Bean
    OpenAPI pulsoApi() {
        return new OpenAPI().info(new Info().title("Pulso API").version("1.0.0").description(
                "Gestão de redes de academias: matrículas, mensalidades, catraca, aulas com fila de espera, "
                        + "risco de evasão e painel executivo. Toda resposta traz os cabeçalhos X-Consultas-Sql e "
                        + "X-Tempo-Ms com o custo da chamada no banco."));
    }
}
