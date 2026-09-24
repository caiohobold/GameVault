package br.edu.unesc.gamevault.dto.response;

import java.time.LocalDateTime;

public record PromocaoResponse(
        Long id,
        JogoResumoResponse jogo,
        Integer percentualDesconto,
        LocalDateTime dataInicio,
        LocalDateTime dataFim,
        Boolean ativa,
        Boolean vigente) {
}
