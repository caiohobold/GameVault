package br.edu.unesc.gamevault.ia;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Map;

import org.junit.jupiter.api.Test;

import br.edu.unesc.gamevault.exception.LimiteDeUsoExcedidoException;
import br.edu.unesc.gamevault.ia.config.IaProperties;

class LimiteDeUsoServiceTest {

    private final LimiteDeUsoService limite = new LimiteDeUsoService(new IaProperties(true, "", "m", Map.of(), 60, 2,
            BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, Map.of("chat", 3), new IaProperties.Resumo(3, 10)));

    @Test
    void bloqueiaDepoisDoLimitePorHora() {
        for (int i = 0; i < 3; i++) {
            limite.registrarChamada(1L, LimiteDeUsoService.CHAT);
        }

        assertThatThrownBy(() -> limite.registrarChamada(1L, LimiteDeUsoService.CHAT))
                .isInstanceOf(LimiteDeUsoExcedidoException.class);
    }

    @Test
    void cadaUsuarioTemOProprioLimite() {
        for (int i = 0; i < 3; i++) {
            limite.registrarChamada(1L, LimiteDeUsoService.CHAT);
        }

        assertThatCode(() -> limite.registrarChamada(2L, LimiteDeUsoService.CHAT)).doesNotThrowAnyException();
    }

    @Test
    void recursoSemLimiteConfiguradoNaoBloqueia() {
        for (int i = 0; i < 50; i++) {
            limite.registrarChamada(1L, LimiteDeUsoService.ASSISTENTE_CADASTRO);
        }
    }
}
