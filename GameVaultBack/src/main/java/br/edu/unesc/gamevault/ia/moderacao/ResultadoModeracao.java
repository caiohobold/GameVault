package br.edu.unesc.gamevault.ia.moderacao;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import br.edu.unesc.gamevault.entity.enums.StatusModeracao;

/** Saída estruturada do Claude ao moderar um comentário. */
public record ResultadoModeracao(
        @JsonPropertyDescription("APROVADA, EM_REVISAO ou BLOQUEADA")
        StatusModeracao decisao,
        @JsonPropertyDescription("true quando o comentário revela final, reviravolta ou morte de personagem")
        boolean contemSpoiler,
        @JsonPropertyDescription("Uma frase em português, escrita para o autor do comentário")
        String motivo) {

    public static final String MOTIVO_SEM_COMENTARIO = "Avaliação sem comentário";
    public static final String MOTIVO_IA_INDISPONIVEL =
            "A moderação automática está indisponível; um administrador vai revisar o comentário.";

    public static ResultadoModeracao aprovadaSemComentario() {
        return new ResultadoModeracao(StatusModeracao.APROVADA, false, MOTIVO_SEM_COMENTARIO);
    }

    /** Sem IA, nada é publicado sem passar por um humano. */
    public static ResultadoModeracao paraRevisaoHumana() {
        return new ResultadoModeracao(StatusModeracao.EM_REVISAO, false, MOTIVO_IA_INDISPONIVEL);
    }

    public boolean aprovada() {
        return decisao == StatusModeracao.APROVADA;
    }

    public boolean bloqueada() {
        return decisao == StatusModeracao.BLOQUEADA;
    }
}
