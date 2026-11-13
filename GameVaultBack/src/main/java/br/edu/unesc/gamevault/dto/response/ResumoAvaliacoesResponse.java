package br.edu.unesc.gamevault.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import br.edu.unesc.gamevault.entity.SentimentoAspecto;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "O que os jogadores dizem do jogo, resumido pela IA")
public record ResumoAvaliacoesResponse(
        JogoResumoResponse jogo,
        @Schema(example = "Os jogadores elogiam a história e a trilha sonora. A principal reclamação é o desempenho no PC.")
        String resumo,
        List<String> pontosPositivos,
        List<String> pontosNegativos,
        List<SentimentoAspecto> aspectos,
        @Schema(description = "Avaliações em que a nota contradiz o texto")
        List<Long> avaliacoesDivergentes,
        @Schema(description = "Quantas avaliações aprovadas existiam quando o resumo foi gerado", example = "24")
        Integer totalAvaliacoesBase,
        LocalDateTime geradoEm) {
}
