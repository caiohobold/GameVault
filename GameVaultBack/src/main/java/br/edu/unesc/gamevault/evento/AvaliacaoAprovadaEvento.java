package br.edu.unesc.gamevault.evento;

/**
 * Publicado quando uma avaliação passa a contar na nota do jogo (aprovada pela IA
 * ou por um administrador). O módulo de resumo decide se precisa refazer o resumo.
 */
public record AvaliacaoAprovadaEvento(Long jogoId) {
}
