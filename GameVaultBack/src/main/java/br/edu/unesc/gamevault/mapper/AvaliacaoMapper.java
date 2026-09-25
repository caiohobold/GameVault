package br.edu.unesc.gamevault.mapper;

import org.springframework.stereotype.Component;

import br.edu.unesc.gamevault.dto.response.AvaliacaoResponse;
import br.edu.unesc.gamevault.entity.Avaliacao;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AvaliacaoMapper {
    private final UsuarioMapper usuarioMapper;
    private final JogoMapper jogoMapper;

    public AvaliacaoResponse paraResposta(Avaliacao avaliacao) {
        return new AvaliacaoResponse(
                avaliacao.getId(),
                usuarioMapper.paraResumo(avaliacao.getUsuario()),
                jogoMapper.paraResumo(avaliacao.getJogo()),
                avaliacao.getNota(),
                avaliacao.getComentario(),
                avaliacao.getDataAvaliacao());
    }
}
