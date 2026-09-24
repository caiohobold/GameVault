package br.edu.unesc.gamevault.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.unesc.gamevault.dto.request.ListaDesejosRequest;
import br.edu.unesc.gamevault.dto.response.ListaDesejosResponse;
import br.edu.unesc.gamevault.entity.Jogo;
import br.edu.unesc.gamevault.entity.ListaDesejos;
import br.edu.unesc.gamevault.entity.Usuario;
import br.edu.unesc.gamevault.exception.RecursoDuplicadoException;
import br.edu.unesc.gamevault.exception.RecursoNaoEncontradoException;
import br.edu.unesc.gamevault.exception.RegraDeNegocioException;
import br.edu.unesc.gamevault.mapper.ListaDesejosMapper;
import br.edu.unesc.gamevault.repository.ListaDesejosRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListaDesejosService {
    private final ListaDesejosRepository listaDesejosRepository;
    private final UsuarioService usuarioService;
    private final JogoService jogoService;
    private final BibliotecaService bibliotecaService;
    private final ListaDesejosMapper listaDesejosMapper;

    @Transactional(readOnly = true)
    public Page<ListaDesejosResponse> listarDoUsuario(Long usuarioId, Pageable paginacao) {
        usuarioService.buscarEntidade(usuarioId);

        return listaDesejosRepository.findByUsuario_Id(usuarioId, paginacao)
                .map(listaDesejosMapper::paraResposta);
    }

    @Transactional
    public ListaDesejosResponse adicionar(ListaDesejosRequest requisicao) {
        Usuario usuario = usuarioService.buscarEntidade(requisicao.usuarioId());
        Jogo jogo = jogoService.buscarEntidadeParaCompra(requisicao.jogoId());

        if (listaDesejosRepository.existsByUsuario_IdAndJogo_Id(usuario.getId(), jogo.getId())) {
            throw new RecursoDuplicadoException(
                    "O jogo '%s' já está na lista de desejos deste usuário".formatted(jogo.getTitulo()));
        }

        if (bibliotecaService.usuarioPossui(usuario.getId(), jogo.getId())) {
            throw new RegraDeNegocioException(
                    "O jogo '%s' já está na biblioteca deste usuário".formatted(jogo.getTitulo()));
        }

        ListaDesejos item = listaDesejosRepository.save(ListaDesejos.builder()
                .usuario(usuario)
                .jogo(jogo)
                .build());

        log.info("Jogo {} adicionado à lista de desejos do usuário {}", jogo.getId(), usuario.getId());
        return listaDesejosMapper.paraResposta(item);
    }

    @Transactional
    public void remover(Long id) {
        ListaDesejos item = listaDesejosRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item da lista de desejos", id));

        listaDesejosRepository.delete(item);
        log.info("Item {} removido da lista de desejos", id);
    }

    @Transactional
    public void removerPorUsuarioEJogo(Long usuarioId, Long jogoId) {
        ListaDesejos item = listaDesejosRepository.findByUsuario_IdAndJogo_Id(usuarioId, jogoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "O jogo %d não está na lista de desejos do usuário %d".formatted(jogoId, usuarioId)));

        listaDesejosRepository.delete(item);
        log.info("Jogo {} removido da lista de desejos do usuário {}", jogoId, usuarioId);
    }
}
