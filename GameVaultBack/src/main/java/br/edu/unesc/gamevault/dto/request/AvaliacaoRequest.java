package br.edu.unesc.gamevault.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Avaliação de um jogo que o usuário possui")
public record AvaliacaoRequest(

        @NotNull(message = "O jogo é obrigatório")
        @Schema(description = "Jogo avaliado; precisa estar na biblioteca de quem avalia", example = "1")
        Long jogoId,

        @NotNull(message = "A nota é obrigatória")
        @Min(value = 1, message = "A nota mínima é 1")
        @Max(value = 5, message = "A nota máxima é 5")
        @Schema(description = "Nota de 1 a 5", example = "4")
        Integer nota,

        @Size(max = 1000, message = "O comentário deve ter no máximo 1000 caracteres")
        @Schema(description = "Comentário opcional", example = "História muito boa, vale cada centavo.")
        String comentario) {
}
