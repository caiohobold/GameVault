package br.edu.unesc.gamevault.entity;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import br.edu.unesc.gamevault.entity.enums.Sentimento;

/**
 * Sentimento dos jogadores sobre um aspecto do jogo. Gravado como JSON dentro de
 * {@code resumo_avaliacao.aspectos} e também usado como saída estruturada do Claude.
 */
public record SentimentoAspecto(
        @JsonPropertyDescription("jogabilidade, gráficos, desempenho, história, trilha sonora, preço, dificuldade ou duração")
        String aspecto,
        @JsonPropertyDescription("POSITIVO, NEGATIVO, NEUTRO ou MISTO")
        Sentimento sentimento,
        @JsonPropertyDescription("Quantas avaliações mencionam o aspecto")
        int mencoes) {
}
