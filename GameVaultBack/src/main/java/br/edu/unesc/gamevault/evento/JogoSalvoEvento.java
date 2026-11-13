package br.edu.unesc.gamevault.evento;

/**
 * Publicado quando um jogo é criado ou editado. Depois do commit, o módulo de
 * busca recalcula o embedding do jogo se o texto mudou.
 */
public record JogoSalvoEvento(Long jogoId) {
}
