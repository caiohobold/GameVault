package br.edu.unesc.gamevault.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Jogo do catálogo. notaMedia e totalAvaliacoes são calculados pelo servidor")
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
