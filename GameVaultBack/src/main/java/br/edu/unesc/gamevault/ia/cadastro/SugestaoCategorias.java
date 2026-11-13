package br.edu.unesc.gamevault.ia.cadastro;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** Saída estruturada do Claude ao sugerir as categorias de um jogo. */
public record SugestaoCategorias(
        @JsonPropertyDescription("Ids das categorias escolhidas, somente da lista disponível")
        List<Long> categoriaIds,
        @JsonPropertyDescription("Uma frase explicando a escolha, para a publicadora")
        String justificativa) {
}
