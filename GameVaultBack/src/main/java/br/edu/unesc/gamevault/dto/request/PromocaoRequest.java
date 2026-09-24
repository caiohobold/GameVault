package br.edu.unesc.gamevault.dto.request;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PromocaoRequest(

        @NotNull(message = "O jogo é obrigatório")
        Long jogoId,

        @NotNull(message = "O percentual de desconto é obrigatório")
        @Min(value = 1, message = "O desconto mínimo é 1%")
        @Max(value = 90, message = "O desconto máximo é 90%")
        Integer percentualDesconto,

        @NotNull(message = "A data de início é obrigatória")
        LocalDateTime dataInicio,

        @NotNull(message = "A data de fim é obrigatória")
        LocalDateTime dataFim,

        Boolean ativa) {
}
