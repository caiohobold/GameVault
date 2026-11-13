package br.edu.unesc.gamevault.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Jogo a ser classificado nas categorias da loja")
public record CategoriasJogoRequest(
        @NotBlank(message = "O título é obrigatório")
        @Size(max = 150, message = "O título deve ter no máximo 150 caracteres")
        @Schema(example = "Fenda Profunda")
        String titulo,
        @Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres")
        @Schema(example = "Acao roguelike em cavernas geradas proceduralmente; cada descida e inedita.")
        String descricao) {
}
