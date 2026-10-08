package br.edu.unesc.gamevault.dto.request;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Desconto por período para um jogo")
public record PromocaoRequest(

        @NotNull(message = "O jogo é obrigatório")
        @Schema(description = "Jogo que entra em promoção", example = "2")
        Long jogoId,

        @NotNull(message = "O percentual de desconto é obrigatório")
        @Min(value = 1, message = "O desconto mínimo é 1%")
        @Max(value = 90, message = "O desconto máximo é 90%")
        @Schema(description = "Percentual entre 1 e 90", example = "50")
        Integer percentualDesconto,

        @NotNull(message = "A data de início é obrigatória")
        @Schema(description = "Início da vigência", example = "2026-10-01T00:00:00")
        LocalDateTime dataInicio,

        @NotNull(message = "A data de fim é obrigatória")
        @Schema(description = "Fim da vigência, posterior ao início", example = "2026-12-31T23:59:59")
        LocalDateTime dataFim,

        Boolean ativa) {
}
