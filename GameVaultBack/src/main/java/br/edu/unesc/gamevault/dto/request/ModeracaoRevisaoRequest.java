package br.edu.unesc.gamevault.dto.request;

import br.edu.unesc.gamevault.entity.enums.StatusModeracao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Decisão do administrador sobre uma avaliação em revisão")
public record ModeracaoRevisaoRequest(
        @NotNull(message = "A decisão é obrigatória")
        @Schema(description = "APROVADA ou BLOQUEADA", example = "APROVADA")
        StatusModeracao decisao,
        @Size(max = 500, message = "O motivo deve ter no máximo 500 caracteres")
        @Schema(description = "Explicação mostrada ao autor", example = "Crítica legítima ao desempenho do jogo")
        String motivo) {
}
