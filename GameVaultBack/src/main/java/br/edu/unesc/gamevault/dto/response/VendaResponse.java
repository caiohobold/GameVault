package br.edu.unesc.gamevault.dto.response;

import java.math.BigDecimal;

public record VendaResponse(
        Long jogoId,
        String titulo,
        Long quantidadeVendida,
        BigDecimal valorArrecadado) {
}
