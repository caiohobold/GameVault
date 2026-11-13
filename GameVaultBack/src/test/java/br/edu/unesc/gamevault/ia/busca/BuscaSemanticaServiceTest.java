package br.edu.unesc.gamevault.ia.busca;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import br.edu.unesc.gamevault.entity.Categoria;

class BuscaSemanticaServiceTest {
    private final List<Categoria> categorias = List.of(
            Categoria.builder().id(1L).nome("Estrategia").build(),
            Categoria.builder().id(2L).nome("Multiplayer").build(),
            Categoria.builder().id(3L).nome("Terror").build());

    @Test
    void aceitaSoCategoriasDoBancoIgnorandoAcentoEMaiusculas() {
        List<Categoria> escolhidas = BuscaSemanticaService.resolverCategorias(
                List.of("Estratégia", "MULTIPLAYER", "Categoria inventada", "estrategia"), categorias);

        assertThat(escolhidas).extracting(Categoria::getId).containsExactly(1L, 2L);
    }

    @Test
    void semCategoriasDevolveListaVazia() {
        assertThat(BuscaSemanticaService.resolverCategorias(null, categorias)).isEmpty();
    }
}
