package br.edu.unesc.gamevault.dto.request;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

@Schema(description = "Itens do pedido. O dono vem do token, não do corpo")
public record PedidoRequest(

        @NotEmpty(message = "O pedido deve ter pelo menos um item")
        @Valid
        @Schema(description = "Jogos que entram no pedido; precisa de pelo menos um")
        List<ItemPedidoRequest> itens) {
}
