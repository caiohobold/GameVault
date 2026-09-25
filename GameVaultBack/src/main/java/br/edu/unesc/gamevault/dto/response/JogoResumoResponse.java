package br.edu.unesc.gamevault.dto.response;

import java.math.BigDecimal;

public record JogoResumoResponse(
        Long id,
        String titulo,
        BigDecimal preco,
        BigDecimal notaMedia,
        Boolean ativo) {
}
