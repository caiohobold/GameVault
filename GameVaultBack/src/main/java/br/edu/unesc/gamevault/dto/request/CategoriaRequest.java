package br.edu.unesc.gamevault.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Categoria do catálogo")
public record CategoriaRequest(

        @NotBlank(message = "O nome da categoria é obrigatório")
        @Size(max = 60, message = "O nome da categoria deve ter no máximo 60 caracteres")
        @Schema(description = "Nome único da categoria", example = "Estratégia")
        String nome,

        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
        @Schema(description = "Texto explicativo", example = "Planejamento, recursos e táticas")
        String descricao) {
}
