package br.edu.unesc.gamevault.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Simulação de promoção feita pelo modelo de previsão de demanda")
public record SimulacaoPromocaoResponse(
        JogoResumoResponse jogo,
        LocalDateTime dataInicio,
        LocalDateTime dataFim,
        List<CenarioPromocaoResponse> cenarios,
        @Schema(description = "Percentual de maior receita; vazio quando não fazer promoção rende mais", example = "30")
        Integer percentualSugerido,
        @Schema(example = "Com 30% de desconto a receita prevista é a maior do período.")
        String recomendacao,
        @Schema(description = "Erro médio absoluto do modelo, em cópias por semana", example = "1.8")
        Double erroMedioModelo) {
}
