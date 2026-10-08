package br.edu.unesc.gamevault.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Edição de uma avaliação existente")
public record AvaliacaoAtualizacaoRequest(

        @NotNull(message = "A nota é obrigatória")
        @Min(value = 1, message = "A nota mínima é 1")
        @Max(value = 5, message = "A nota máxima é 5")
        @Schema(description = "Nota de 1 a 5", example = "5")
        Integer nota,

        @Size(max = 1000, message = "O comentário deve ter no máximo 1000 caracteres")
        @Schema(description = "Comentário opcional", example = "Mudei de ideia: é excelente.")
        String comentario) {
}
