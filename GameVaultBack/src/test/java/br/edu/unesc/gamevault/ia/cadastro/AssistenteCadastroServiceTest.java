package br.edu.unesc.gamevault.ia.cadastro;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.LongStream;

import org.junit.jupiter.api.Test;

import br.edu.unesc.gamevault.entity.Categoria;

class AssistenteCadastroServiceTest {
    private final List<Categoria> categorias = LongStream.rangeClosed(1, 10)
            .mapToObj(id -> Categoria.builder().id(id).nome("Categoria " + id).build())
            .toList();

    @Test
    void descartaIdsInventadosERepetidosEFicaComNoMaximoQuatro() {
        List<Categoria> escolhidas = AssistenteCadastroService.filtrarExistentes(
                List.of(3L, 99L, 3L, 1L, 7L, 8L, 9L), categorias);

        assertThat(escolhidas).extracting(Categoria::getId).containsExactly(3L, 1L, 7L, 8L);
    }
}
