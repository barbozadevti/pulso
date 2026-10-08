package dev.barboza.pulso;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import dev.barboza.pulso.repositorio.AulaRepository;
import dev.barboza.pulso.servico.LaboratorioService;
import dev.barboza.pulso.servico.LaboratorioService.Disputa;

/**
 * Sem @Transactional de propósito: a corrida só é real com transações de verdade, em threads diferentes,
 * cada uma com seu commit. É o teste que prova o bloqueio otimista (@Version) em ação.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(RelogioFixo.class)
class DisputaDeVagasTest {

    @Autowired
    LaboratorioService laboratorio;
    @Autowired
    AulaRepository aulas;

    @Test
    void quarentaThreadsDisputandoTresVagasNuncaGeramOverbooking() {
        long aulasAntes = aulas.count();
        for (int rodada = 0; rodada < 3; rodada++) {
            Disputa d = laboratorio.disputar(40, 3);
            assertThat(d.overbooking()).isFalse();
            assertThat(d.confirmadas()).isEqualTo(3);
            assertThat(d.naEspera()).isEqualTo(37);
            assertThat(d.semResposta()).isZero();
            assertThat(d.conflitosResolvidos()).isGreaterThan(0); // houve corrida de verdade e o retry resolveu
        }
        assertThat(aulas.count()).isEqualTo(aulasAntes); // a aula descartável foi removida
    }
}
