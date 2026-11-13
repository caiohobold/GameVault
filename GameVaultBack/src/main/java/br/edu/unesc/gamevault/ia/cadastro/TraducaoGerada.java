package br.edu.unesc.gamevault.ia.cadastro;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** Saída estruturada do Claude ao traduzir uma descrição. */
public record TraducaoGerada(
        @JsonPropertyDescription("Texto traduzido, sem comentários sobre a tradução")
        String texto) {
}
