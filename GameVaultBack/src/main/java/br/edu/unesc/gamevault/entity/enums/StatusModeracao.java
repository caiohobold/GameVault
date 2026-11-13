package br.edu.unesc.gamevault.entity.enums;

/** Situação de uma avaliação depois da moderação (pela IA ou por um administrador). */
public enum StatusModeracao {
    /** Publicada: aparece na página do jogo e conta na nota média. */
    APROVADA,

    /** Aguardando um administrador: não aparece nem conta na nota. */
    EM_REVISAO,

    /** Recusada: não aparece nem conta na nota. */
    BLOQUEADA
}
