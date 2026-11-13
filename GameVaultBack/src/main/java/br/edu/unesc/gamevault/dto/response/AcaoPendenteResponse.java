package br.edu.unesc.gamevault.dto.response;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
        Ação que o assistente preparou e que o usuário precisa confirmar na tela. O assistente nunca
        paga sozinho: o botão de confirmação chama POST /pedidos/{pedidoId}/pagar.
        """)
public record AcaoPendenteResponse(
        @Schema(example = "PAGAR_PEDIDO") String tipo,
        @Schema(example = "42") Long pedidoId,
        @Schema(example = "149.90") BigDecimal valor,
        @Schema(example = "Confirmar o pagamento do pedido #42 no valor de R$ 149,90") String descricao) {
}
