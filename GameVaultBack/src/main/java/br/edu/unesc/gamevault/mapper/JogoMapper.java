package br.edu.unesc.gamevault.mapper;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Component;

import br.edu.unesc.gamevault.dto.response.CategoriaResponse;
import br.edu.unesc.gamevault.dto.response.JogoResponse;
import br.edu.unesc.gamevault.dto.response.JogoResumoResponse;
import br.edu.unesc.gamevault.entity.Jogo;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class JogoMapper {
    private final CategoriaMapper categoriaMapper;
    private final UsuarioMapper usuarioMapper;

    public JogoResponse paraResposta(Jogo jogo) {
        List<CategoriaResponse> categorias = jogo.getCategorias().stream()
                .map(categoriaMapper::paraResposta)
                .sorted(Comparator.comparing(CategoriaResponse::nome))
                .toList();

        return new JogoResponse(
                jogo.getId(),
                jogo.getTitulo(),
                jogo.getDescricao(),
                jogo.getPreco(),
                jogo.getDataLancamento(),
                usuarioMapper.paraResumo(jogo.getPublicadora()),
                jogo.getNotaMedia(),
                jogo.getTotalAvaliacoes(),
                jogo.getAtivo(),
                categorias);
    }

    public JogoResumoResponse paraResumo(Jogo jogo) {
        return new JogoResumoResponse(
                jogo.getId(),
                jogo.getTitulo(),
                jogo.getPreco(),
                jogo.getNotaMedia(),
                jogo.getAtivo());
    }
}
