package br.edu.unesc.gamevault.dto.request;

import jakarta.validation.constraints.NotNull;

public record ItemPedidoRequest(

        @NotNull(message = "O jogo é obrigatório")
        Long jogoId) {
}
