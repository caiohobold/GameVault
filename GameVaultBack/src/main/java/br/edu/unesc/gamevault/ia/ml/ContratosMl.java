package br.edu.unesc.gamevault.ia.ml;

import java.time.LocalDate;
import java.util.List;

/**
 * Formato das mensagens trocadas com o GameVaultML. Ficam aqui, separados dos DTOs
 * da API, porque são um contrato interno entre os dois serviços.
 */
public final class ContratosMl {
    private ContratosMl() {
    }

    public record EmbeddingsRequisicao(List<String> textos) {
    }

    public record EmbeddingsResposta(List<float[]> vetores, String modelo, int dimensoes) {
    }

    public record RecomendacaoMl(Long usuarioId, Long jogoId, double pontuacao, Long motivoJogoId) {
    }

    public record SimilarMl(Long jogoId, Long similarId, double pontuacao) {
    }

    public record MetricasRecomendacao(Double hitRate10, Double hitRate10Popularidade, int usuariosAvaliados,
            int usuarios, int jogos) {
    }

    public record TreinoRecomendacaoResposta(
            List<RecomendacaoMl> recomendacoes,
            List<SimilarMl> similares,
            MetricasRecomendacao metricas) {
    }

    public record JanelaMl(LocalDate inicio, LocalDate fim, int percentualDesconto) {
    }

    public record PrevisaoDemandaRequisicao(Long jogoId, List<JanelaMl> janelas) {
    }

    public record PrevisaoJanelaMl(LocalDate inicio, LocalDate fim, int percentualDesconto,
            double vendasPrevistas) {
    }

    public record PrevisaoDemandaResposta(Long jogoId, List<PrevisaoJanelaMl> previsoes, Double mae) {
    }

    public record TreinoDemandaResposta(Double mae, int amostras) {
    }

    public record UsuarioSuspeitoMl(Long usuarioId, double pontuacao, List<String> motivos) {
    }

    public record DeteccaoAnomaliasResposta(
            List<UsuarioSuspeitoMl> suspeitos,
            int usuariosAnalisados,
            boolean classificadorSupervisionado) {
    }
}
