package br.edu.unesc.gamevault.ia.ml;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import br.edu.unesc.gamevault.exception.IaIndisponivelException;
import br.edu.unesc.gamevault.ia.ml.ContratosMl.DeteccaoAnomaliasResposta;
import br.edu.unesc.gamevault.ia.ml.ContratosMl.EmbeddingsRequisicao;
import br.edu.unesc.gamevault.ia.ml.ContratosMl.EmbeddingsResposta;
import br.edu.unesc.gamevault.ia.ml.ContratosMl.PrevisaoDemandaRequisicao;
import br.edu.unesc.gamevault.ia.ml.ContratosMl.PrevisaoDemandaResposta;
import br.edu.unesc.gamevault.ia.ml.ContratosMl.TreinoDemandaResposta;
import br.edu.unesc.gamevault.ia.ml.ContratosMl.TreinoRecomendacaoResposta;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Cliente HTTP do serviço Python GameVaultML, que roda os modelos de aprendizado de
 * máquina e gera os embeddings. O serviço só calcula: quem grava os resultados e
 * aplica as regras de negócio é o backend.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClienteML {
    private final RestClient clienteHttpMl;

    public List<float[]> gerarEmbeddings(List<String> textos) {
        EmbeddingsResposta resposta = post("/embeddings", new EmbeddingsRequisicao(textos), EmbeddingsResposta.class);
        return resposta.vetores();
    }

    public float[] gerarEmbedding(String texto) {
        return gerarEmbeddings(List.of(texto)).get(0);
    }

    public TreinoRecomendacaoResposta treinarRecomendacoes() {
        return post("/recomendacao/treinar", null, TreinoRecomendacaoResposta.class);
    }

    public PrevisaoDemandaResposta preverDemanda(PrevisaoDemandaRequisicao requisicao) {
        return post("/demanda/prever", requisicao, PrevisaoDemandaResposta.class);
    }

    public TreinoDemandaResposta treinarDemanda() {
        return post("/demanda/treinar", null, TreinoDemandaResposta.class);
    }

    public DeteccaoAnomaliasResposta detectarAnomalias() {
        return post("/anomalias/detectar", null, DeteccaoAnomaliasResposta.class);
    }

    private <T> T post(String rota, Object corpo, Class<T> tipo) {
        try {
            RestClient.RequestBodySpec requisicao = clienteHttpMl.post()
                    .uri(rota)
                    .contentType(MediaType.APPLICATION_JSON);

            if (corpo != null) {
                requisicao.body(corpo);
            }

            T resposta = requisicao.retrieve().body(tipo);

            if (resposta == null) {
                throw new IaIndisponivelException("O serviço de aprendizado de máquina devolveu uma resposta vazia.");
            }
            return resposta;
        } catch (RestClientException excecao) {
            log.warn("GameVaultML indisponivel em {}: {}", rota, excecao.getMessage());
            throw new IaIndisponivelException("O serviço de aprendizado de máquina está indisponível no momento.",
                    excecao);
        }
    }
}
