package br.edu.unesc.gamevault.dto.response;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Previsão do modelo para um percentual de desconto")
public record CenarioPromocaoResponse(
        @Schema(example = "30") Integer percentualDesconto,
        @Schema(example = "104.93") BigDecimal precoComDesconto,
        @Schema(description = "Cópias previstas no período", example = "41.5") Double vendasPrevistas,
        @Schema(example = "4354.60") BigDecimal receitaEstimada,
        @Schema(description = "true no cenário de maior receita") Boolean sugerido) {
}
