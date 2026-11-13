package br.edu.unesc.gamevault.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Jogo sugerido pelo modelo de recomendação")
public record RecomendacaoResponse(
        JogoResponse jogo,
        @Schema(description = "Quanto maior, mais o modelo acredita que o jogo combina", example = "0.83")
        Double pontuacao,
        @Schema(description = "Explicação para o usuário", example = "Porque você jogou Sombras de Aldoria")
        String motivo,
        @Schema(description = "FILTRAGEM_COLABORATIVA (histórico de quem tem gosto parecido) ou CONTEUDO "
                + "(semelhança de texto com jogos que o usuário favoritou ou visitou)",
                example = "FILTRAGEM_COLABORATIVA")
        String origem) {
}
