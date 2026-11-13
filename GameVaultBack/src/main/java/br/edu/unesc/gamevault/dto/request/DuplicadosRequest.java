package br.edu.unesc.gamevault.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Jogo sendo cadastrado, para procurar cadastros parecidos")
public record DuplicadosRequest(
        @NotBlank(message = "O título é obrigatório")
        @Size(max = 150, message = "O título deve ter no máximo 150 caracteres")
        String titulo,
        @Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres")
        String descricao,
        @Schema(description = "Na edição, o próprio jogo, que não conta como duplicado", example = "4")
        Long ignorarJogoId) {
}
