package br.edu.unesc.gamevault.ia.resumo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import br.edu.unesc.gamevault.entity.SentimentoAspecto;

/** Saída estruturada do Claude ao resumir as avaliações de um jogo. */
public record ResumoGerado(
        @JsonPropertyDescription("2 ou 3 frases com o consenso e a divergência mais relevante")
        String resumo,
        @JsonPropertyDescription("Até 5 pontos positivos citados em mais de uma avaliação")
        List<String> pontosPositivos,
        @JsonPropertyDescription("Até 5 pontos negativos citados em mais de uma avaliação")
        List<String> pontosNegativos,
        @JsonPropertyDescription("Sentimento sobre cada aspecto mencionado")
        List<SentimentoAspecto> aspectos,
        @JsonPropertyDescription("Ids das avaliações em que a nota contradiz claramente o texto")
        List<Long> avaliacoesDivergentes) {
}
