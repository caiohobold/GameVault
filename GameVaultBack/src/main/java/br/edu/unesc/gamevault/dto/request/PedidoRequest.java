package br.edu.unesc.gamevault.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record PedidoRequest(

        @NotNull(message = "O usuário é obrigatório")
        Long usuarioId,

        @NotEmpty(message = "O pedido deve ter pelo menos um item")
        @Valid
        List<ItemPedidoRequest> itens) {
}
