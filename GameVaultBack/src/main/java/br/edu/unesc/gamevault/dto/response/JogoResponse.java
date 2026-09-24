package br.edu.unesc.gamevault.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record JogoResponse(
        Long id,
        String titulo,
        String descricao,
        BigDecimal preco,
        LocalDate dataLancamento,
        UsuarioResumoResponse publicadora,
        BigDecimal notaMedia,
        Integer totalAvaliacoes,
        Boolean ativo,
        List<CategoriaResponse> categorias) {
}
