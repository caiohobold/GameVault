package br.edu.unesc.gamevault.ia.busca;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** Saída estruturada do Claude ao interpretar a frase digitada na busca. */
public record FiltrosBusca(
        @JsonPropertyDescription("Descrição limpa do tipo de jogo procurado, para a busca por significado")
        String textoSemantico,
        @JsonPropertyDescription("Preço máximo em reais pedido na frase; 0 quando a frase não fala em preço")
        double precoMaximo,
        @JsonPropertyDescription("Nomes de categorias pedidas com clareza, exatamente como na lista disponível")
        List<String> categorias,
        @JsonPropertyDescription("true somente quando a frase pede promoção, desconto ou oferta")
        boolean apenasEmPromocao) {
}
