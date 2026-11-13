package br.edu.unesc.gamevault.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado de um treino do modelo de previsão de demanda")
public record TreinoDemandaResponse(
        @Schema(description = "Erro médio absoluto nos 20% de dados separados para teste", example = "1.8")
        Double erroMedioAbsoluto,
        @Schema(example = "1240") Integer amostras) {
}
