package br.edu.unesc.gamevault.ia.claude;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.anthropic.client.AnthropicClient;

import br.edu.unesc.gamevault.exception.IaIndisponivelException;
import br.edu.unesc.gamevault.ia.config.IaProperties;
import br.edu.unesc.gamevault.ia.moderacao.ResultadoModeracao;

@ExtendWith(MockitoExtension.class)
class ClienteClaudeTest {
    @Mock
    private AnthropicClient anthropic;
    @Mock
    private CarregadorDePrompts prompts;
    @Mock
    private RegistroDeUsoIa registro;

    private static IaProperties propriedades(String chave) {
        return new IaProperties(true, chave, "claude-opus-5-5", Map.of(), 60, 2, new BigDecimal("4"),
                new BigDecimal("20"), new BigDecimal("0.2"), Map.of(), new IaProperties.Resumo(3, 10));
    }

    @Test
    void semChaveNaoChamaAApiERespondeIndisponivel() {
        ClienteClaude cliente = new ClienteClaude(anthropic, propriedades(""), prompts, registro);

        assertThatThrownBy(() -> cliente.gerarEstruturado(ModuloIa.MODERACAO, "<comentario>oi</comentario>",
                ResultadoModeracao.class, 1L))
                .isInstanceOf(IaIndisponivelException.class)
                .hasMessageContaining("ANTHROPIC_API_KEY");

        assertThatThrownBy(() -> cliente.conversar(ModuloIa.ASSISTENTE_CHAT, List.of(), List.of(), 1L))
                .isInstanceOf(IaIndisponivelException.class);

        verifyNoInteractions(anthropic, registro);
    }

    @Test
    void trocaDeModeloPorModuloVemDaConfiguracao() {
        IaProperties comTroca = new IaProperties(true, "x", "claude-opus-5-5", Map.of("MODERACAO", "claude-haiku-5-5"),
                60, 2, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, Map.of(), new IaProperties.Resumo(3, 10));

        org.assertj.core.api.Assertions.assertThat(comTroca.modeloDe(ModuloIa.MODERACAO)).isEqualTo("claude-haiku-5-5");
        org.assertj.core.api.Assertions.assertThat(comTroca.modeloDe(ModuloIa.ASSISTENTE_CHAT)).isEqualTo("claude-opus-5-5");
    }
}
