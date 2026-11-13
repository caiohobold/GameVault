package br.edu.unesc.gamevault.dto.response;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Métrica de avaliação de um modelo de aprendizado de máquina no último treino")
public record MetricaModeloResponse(
        @Schema(example = "RECOMENDACAO") String modelo,
        @Schema(example = "HIT_RATE_10") String metrica,
        @Schema(example = "0.42") Double valor,
        @Schema(description = "Quantidade de exemplos usados no cálculo", example = "140") Integer amostras,
        LocalDateTime calculadaEm) {
}
