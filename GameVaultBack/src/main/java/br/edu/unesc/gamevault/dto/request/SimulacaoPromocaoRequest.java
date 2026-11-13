package br.edu.unesc.gamevault.dto.request;

import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Período em que a publicadora pensa em fazer uma promoção")
public record SimulacaoPromocaoRequest(
        @NotNull(message = "O jogo é obrigatório")
        @Schema(example = "2")
        Long jogoId,
        @NotNull(message = "A data de início é obrigatória")
        @Schema(example = "2026-12-01T00:00:00")
        LocalDateTime dataInicio,
        @NotNull(message = "A data de fim é obrigatória")
        @Schema(example = "2026-12-08T00:00:00")
        LocalDateTime dataFim) {
}
