package br.edu.unesc.gamevault.mapper;

import org.springframework.stereotype.Component;

import br.edu.unesc.gamevault.dto.request.CategoriaRequest;
import br.edu.unesc.gamevault.dto.response.CategoriaResponse;
import br.edu.unesc.gamevault.entity.Categoria;

@Component
public class CategoriaMapper {
    public Categoria paraEntidade(CategoriaRequest requisicao) {
        return Categoria.builder()
                .nome(requisicao.nome().trim())
                .descricao(normalizar(requisicao.descricao()))
                .build();
    }

    public void copiarParaEntidade(CategoriaRequest requisicao, Categoria categoria) {
        categoria.setNome(requisicao.nome().trim());
        categoria.setDescricao(normalizar(requisicao.descricao()));
    }

    public CategoriaResponse paraResposta(Categoria categoria) {
        return new CategoriaResponse(
                categoria.getId(),
                categoria.getNome(),
                categoria.getDescricao());
    }

    private String normalizar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpo = texto.trim();
        return limpo.isEmpty() ? null : limpo;
    }
}
