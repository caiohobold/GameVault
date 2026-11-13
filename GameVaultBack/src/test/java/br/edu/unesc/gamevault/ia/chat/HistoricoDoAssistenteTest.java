package br.edu.unesc.gamevault.ia.chat;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.TextBlockParam;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUseBlockParam;

/** O histórico do chat é gravado em JSONB e reenviado sem alteração: a ida e volta não pode perder nada. */
class HistoricoDoAssistenteTest {

    private final MessageParam respostaComFerramenta = MessageParam.ofAssistantOfContentBlockParams(List.of(
            ContentBlockParam.ofText(TextBlockParam.builder().text("Vou consultar seu saldo.").build()),
            ContentBlockParam.ofToolUse(ToolUseBlockParam.builder()
                    .id("toolu_01")
                    .name("meu_saldo")
                    .input(JsonValue.from(Map.of()))
                    .build())));

    private final MessageParam resultadoDaFerramenta = MessageParam.ofUserOfContentBlockParams(List.of(
            ContentBlockParam.ofToolResult(ToolResultBlockParam.builder()
                    .toolUseId("toolu_01")
                    .content("{\"saldo\":250.00}")
                    .build())));

    /**
     * O que vai para a API é o JSON: depois de gravado e lido, a mensagem precisa gerar
     * exatamente o mesmo JSON (o SDK pode trocar a representação interna do input da
     * ferramenta, mas não o conteúdo enviado).
     */
    @Test
    void mensagensVoltamDoBancoComOMesmoJson() {
        for (MessageParam mensagem : List.of(MessageParam.ofUser("oi"), respostaComFerramenta, resultadoDaFerramenta)) {
            String json = AssistenteChatService.serializar(mensagem);
            MessageParam lida = AssistenteChatService.desserializar(json);

            assertThat(AssistenteChatService.serializar(lida)).isEqualTo(json);
            assertThat(lida.isAssistant()).isEqualTo(mensagem.isAssistant());
        }
    }

    @Test
    void naTelaSoAparecemOsTextos() {
        assertThat(AssistenteChatService.textoVisivel(MessageParam.ofUser("tem promoção?"))).isEqualTo("tem promoção?");
        assertThat(AssistenteChatService.textoVisivel(respostaComFerramenta)).isEqualTo("Vou consultar seu saldo.");
        assertThat(AssistenteChatService.textoVisivel(resultadoDaFerramenta)).isEmpty();
    }
}
