package br.edu.unesc.gamevault.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Uso do Claude por módulo e por dia")
public record UsoIaResponse(
        @Schema(example = "MODERACAO") String modulo,
        LocalDate dia,
        @Schema(example = "57") Long chamadas,
        @Schema(example = "1") Long falhas,
        @Schema(example = "48210") Long tokensEntrada,
        @Schema(example = "3120") Long tokensSaida,
        @Schema(example = "0") Long tokensCacheLeitura,
        @Schema(description = "Estimativa pelo preço configurado do modelo padrão", example = "0.2553")
        BigDecimal custoEstimadoUsd) {
}
