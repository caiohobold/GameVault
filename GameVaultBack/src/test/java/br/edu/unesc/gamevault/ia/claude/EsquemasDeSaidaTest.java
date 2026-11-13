package br.edu.unesc.gamevault.ia.claude;

import static org.assertj.core.api.Assertions.assertThatCode;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.anthropic.core.JsonSchemaLocalValidation;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StructuredOutputConfig;

import br.edu.unesc.gamevault.ia.busca.FiltrosBusca;
import br.edu.unesc.gamevault.ia.cadastro.DescricaoGerada;
import br.edu.unesc.gamevault.ia.cadastro.SugestaoCategorias;
import br.edu.unesc.gamevault.ia.cadastro.TraducaoGerada;
import br.edu.unesc.gamevault.ia.moderacao.ResultadoModeracao;
import br.edu.unesc.gamevault.ia.resumo.ResumoGerado;

/**
 * O SDK valida localmente se cada record vira um esquema JSON aceito pela saída
 * estruturada da API. Sem chave essa validação nunca rodaria nos fluxos normais,
 * então ela é exercitada aqui, sem chamada de rede.
 */
class EsquemasDeSaidaTest {

    static Stream<Class<?>> esquemas() {
        return Stream.of(FiltrosBusca.class, ResultadoModeracao.class, ResumoGerado.class, DescricaoGerada.class,
                SugestaoCategorias.class, TraducaoGerada.class);
    }

    @ParameterizedTest
    @MethodSource("esquemas")
    <T> void recordGeraEsquemaValido(Class<T> tipo) {
        assertThatCode(() -> MessageCreateParams.builder()
                .model("claude-opus-5-5")
                .maxTokens(1000L)
                .addUserMessage("teste")
                .outputConfig(StructuredOutputConfig.<T>builder()
                        .effort(OutputConfig.Effort.LOW)
                        .format(tipo, JsonSchemaLocalValidation.YES)
                        .build())
                .build())
                .doesNotThrowAnyException();
    }
}
