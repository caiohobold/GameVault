package br.edu.unesc.gamevault.ia.cadastro;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** Saída estruturada do Claude ao escrever a descrição de um jogo. */
public record DescricaoGerada(
        @JsonPropertyDescription("Descrição do jogo em português, texto corrido, até 1500 caracteres")
        String descricao) {
}
