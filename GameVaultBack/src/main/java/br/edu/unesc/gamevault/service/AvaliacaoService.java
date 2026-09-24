package br.edu.unesc.gamevault.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.unesc.gamevault.dto.request.AvaliacaoAtualizacaoRequest;
import br.edu.unesc.gamevault.dto.request.AvaliacaoRequest;
import br.edu.unesc.gamevault.dto.response.AvaliacaoResponse;
import br.edu.unesc.gamevault.entity.Avaliacao;
import br.edu.unesc.gamevault.entity.Jogo;
import br.edu.unesc.gamevault.entity.Usuario;
import br.edu.unesc.gamevault.entity.enums.Role;
import br.edu.unesc.gamevault.exception.AcessoNegadoException;
import br.edu.unesc.gamevault.exception.RecursoDuplicadoException;
import br.edu.unesc.gamevault.exception.RecursoNaoEncontradoException;
import br.edu.unesc.gamevault.mapper.AvaliacaoMapper;
import br.edu.unesc.gamevault.repository.AvaliacaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvaliacaoService {
    private final AvaliacaoRepository avaliacaoRepository;
    private final UsuarioService usuarioService;
    private final JogoService jogoService;
    private final BibliotecaService bibliotecaService;
    private final AvaliacaoMapper avaliacaoMapper;

    @Transactional(readOnly = true)
    public Page<AvaliacaoResponse> listarPorJogo(Long jogoId, Pageable paginacao) {
        jogoService.buscarEntidade(jogoId);

        return avaliacaoRepository.findByJogo_Id(jogoId, paginacao).map(avaliacaoMapper::paraResposta);
    }

    @Transactional(readOnly = true)
    public Page<AvaliacaoResponse> listarPorUsuario(Long usuarioId, Pageable paginacao) {
        usuarioService.buscarEntidade(usuarioId);

        return avaliacaoRepository.findByUsuario_Id(usuarioId, paginacao).map(avaliacaoMapper::paraResposta);
    }

    @Transactional(readOnly = true)
    public AvaliacaoResponse buscarPorId(Long id) {
        return avaliacaoMapper.paraResposta(buscarEntidade(id));
    }

    @Transactional
    public AvaliacaoResponse criar(AvaliacaoRequest requisicao) {
        Usuario usuario = usuarioService.buscarEntidade(requisicao.usuarioId());
        Jogo jogo = jogoService.buscarEntidade(requisicao.jogoId());

        if (!bibliotecaService.usuarioPossui(usuario.getId(), jogo.getId())) {
            throw new AcessoNegadoException(
                    "O usuário '%s' não possui o jogo '%s' e por isso não pode avaliá-lo"
                            .formatted(usuario.getNome(), jogo.getTitulo()));
        }

        if (avaliacaoRepository.existsByUsuario_IdAndJogo_Id(usuario.getId(), jogo.getId())) {
            throw new RecursoDuplicadoException(
                    "O usuário '%s' já avaliou o jogo '%s'".formatted(usuario.getNome(), jogo.getTitulo()));
        }

        Avaliacao avaliacao = Avaliacao.builder()
                .usuario(usuario)
                .jogo(jogo)
                .nota(requisicao.nota())
                .comentario(normalizar(requisicao.comentario()))
                .build();

        avaliacao = avaliacaoRepository.save(avaliacao);
        recalcularEstatisticas(jogo.getId());

        log.info("Avaliação criada: id={} usuario={} jogo={} nota={}", avaliacao.getId(),
                usuario.getId(), jogo.getId(), avaliacao.getNota());

        return avaliacaoMapper.paraResposta(avaliacao);
    }

    @Transactional
    public AvaliacaoResponse atualizar(Long id, AvaliacaoAtualizacaoRequest requisicao,
            Long solicitanteId) {
        Avaliacao avaliacao = buscarEntidade(id);
        validarAutor(avaliacao, solicitanteId);

        avaliacao.setNota(requisicao.nota());
        avaliacao.setComentario(normalizar(requisicao.comentario()));

        avaliacaoRepository.flush();
        recalcularEstatisticas(avaliacao.getJogo().getId());

        log.info("Avaliação atualizada: id={} nota={}", id, requisicao.nota());
        return avaliacaoMapper.paraResposta(avaliacao);
    }

    @Transactional
    public void excluir(Long id, Long solicitanteId) {
        Avaliacao avaliacao = buscarEntidade(id);
        validarAutor(avaliacao, solicitanteId);

        Long jogoId = avaliacao.getJogo().getId();

        avaliacaoRepository.delete(avaliacao);
        avaliacaoRepository.flush();
        recalcularEstatisticas(jogoId);

        log.info("Avaliação excluída: id={}", id);
    }

    @Transactional(readOnly = true)
    public Avaliacao buscarEntidade(Long id) {
        return avaliacaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Avaliação", id));
    }

    private void recalcularEstatisticas(Long jogoId) {
        Double media = avaliacaoRepository.calcularMediaDoJogo(jogoId);
        long total = avaliacaoRepository.countByJogo_Id(jogoId);

        jogoService.atualizarEstatisticas(jogoId, media, total);
    }

    private void validarAutor(Avaliacao avaliacao, Long solicitanteId) {
        if (solicitanteId == null) {
            return;
        }

        Usuario solicitante = usuarioService.buscarEntidade(solicitanteId);

        if (solicitante.getRole() == Role.ADMIN) {
            return;
        }

        if (!avaliacao.getUsuario().getId().equals(solicitanteId)) {
            throw new AcessoNegadoException(
                    "O usuário %d não é o autor desta avaliação".formatted(solicitanteId));
        }
    }

    private String normalizar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpo = texto.trim();
        return limpo.isEmpty() ? null : limpo;
    }
}
