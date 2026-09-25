package br.edu.unesc.gamevault.mapper;

import org.springframework.stereotype.Component;

import br.edu.unesc.gamevault.dto.response.BibliotecaResponse;
import br.edu.unesc.gamevault.entity.Biblioteca;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BibliotecaMapper {
    private final UsuarioMapper usuarioMapper;
    private final JogoMapper jogoMapper;

    public BibliotecaResponse paraResposta(Biblioteca biblioteca) {
        return new BibliotecaResponse(
                biblioteca.getId(),
                usuarioMapper.paraResumo(biblioteca.getUsuario()),
                jogoMapper.paraResumo(biblioteca.getJogo()),
                biblioteca.getDataAquisicao(),
                biblioteca.getHorasJogadas());
    }
}
