package br.edu.unesc.gamevault.mapper;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import br.edu.unesc.gamevault.dto.response.PromocaoResponse;
import br.edu.unesc.gamevault.entity.Promocao;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class PromocaoMapper {
    private final JogoMapper jogoMapper;

    public PromocaoResponse paraResposta(Promocao promocao) {
        return new PromocaoResponse(
                promocao.getId(),
                jogoMapper.paraResumo(promocao.getJogo()),
                promocao.getPercentualDesconto(),
                promocao.getDataInicio(),
                promocao.getDataFim(),
                promocao.getAtiva(),
                promocao.estaVigenteEm(LocalDateTime.now()));
    }
}
