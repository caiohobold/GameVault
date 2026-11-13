package br.edu.unesc.gamevault.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado de um treino do modelo de recomendação")
public record TreinoRecomendacaoResponse(
        @Schema(example = "2860") Integer recomendacoesGeradas,
        @Schema(example = "300") Integer paresDeJogosSimilares,
        @Schema(description = "Taxa de acerto nos 10 primeiros, medida separando um jogo de cada usuário",
                example = "0.41")
        Double hitRate10,
        @Schema(description = "A mesma medida recomendando só os mais vendidos; o modelo precisa superar este valor",
                example = "0.47")
        Double hitRate10Popularidade,
        @Schema(example = "140") Integer usuariosAvaliados) {
}
