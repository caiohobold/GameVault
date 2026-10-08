package br.edu.unesc.gamevault.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Jogo adicionado à lista de desejos")
public record ListaDesejosRequest(

        @NotNull(message = "O jogo é obrigatório")
        @Schema(description = "Identificador do jogo", example = "2")
        Long jogoId) {
}
