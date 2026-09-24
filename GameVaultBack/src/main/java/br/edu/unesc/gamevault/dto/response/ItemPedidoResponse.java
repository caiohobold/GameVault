package br.edu.unesc.gamevault.dto.response;

import java.math.BigDecimal;

public record ItemPedidoResponse(
        Long id,
        JogoResumoResponse jogo,
        BigDecimal precoUnitario,
        BigDecimal descontoAplicado,
        BigDecimal subtotal) {
}
