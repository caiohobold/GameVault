package br.edu.unesc.gamevault.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Jogo incluído no pedido")
public record ItemPedidoRequest(

        @NotNull(message = "O jogo é obrigatório")
        @Schema(description = "Identificador do jogo", example = "1")
        Long jogoId) {
}
