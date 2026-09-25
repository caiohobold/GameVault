package br.edu.unesc.gamevault.mapper;

import org.springframework.stereotype.Component;

import br.edu.unesc.gamevault.dto.response.ListaDesejosResponse;
import br.edu.unesc.gamevault.entity.ListaDesejos;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class ListaDesejosMapper {
    private final UsuarioMapper usuarioMapper;
    private final JogoMapper jogoMapper;

    public ListaDesejosResponse paraResposta(ListaDesejos item) {
        return new ListaDesejosResponse(
                item.getId(),
                usuarioMapper.paraResumo(item.getUsuario()),
                jogoMapper.paraResumo(item.getJogo()),
                item.getDataAdicao());
    }
}
