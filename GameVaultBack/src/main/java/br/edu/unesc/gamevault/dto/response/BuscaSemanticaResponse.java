package br.edu.unesc.gamevault.dto.response;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado da busca em linguagem natural")
public record BuscaSemanticaResponse(
        @Schema(description = "Frase digitada", example = "estratégia até 50 reais pra jogar com amigos")
        String consulta,
        @Schema(description = "SEMANTICA quando a busca usou embeddings; TEXTUAL quando caiu na busca por título",
                example = "SEMANTICA")
        String modo,
        FiltrosAplicados filtrosAplicados,
        Page<JogoResponse> resultados) {

    @Schema(description = "O que foi entendido da frase e aplicado como filtro")
    public record FiltrosAplicados(
            @Schema(example = "estratégia multiplayer cooperativo") String textoInterpretado,
            @Schema(example = "50.00") BigDecimal precoMaximo,
            List<CategoriaResponse> categorias,
            Boolean apenasEmPromocao,
            @Schema(description = "false quando o Claude não estava disponível e a frase foi usada como veio")
            Boolean interpretadoPorIa) {
    }
}
