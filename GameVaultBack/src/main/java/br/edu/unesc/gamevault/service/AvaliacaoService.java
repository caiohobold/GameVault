package br.edu.unesc.gamevault.service;

import java.time.LocalDateTime;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import br.edu.unesc.gamevault.dto.request.AvaliacaoAtualizacaoRequest;
import br.edu.unesc.gamevault.dto.request.AvaliacaoRequest;
import br.edu.unesc.gamevault.dto.request.ModeracaoRevisaoRequest;
import br.edu.unesc.gamevault.dto.response.AvaliacaoResponse;
import br.edu.unesc.gamevault.entity.Avaliacao;
import br.edu.unesc.gamevault.entity.Jogo;
import br.edu.unesc.gamevault.entity.Usuario;
import br.edu.unesc.gamevault.entity.enums.Role;
import br.edu.unesc.gamevault.entity.enums.StatusModeracao;
import br.edu.unesc.gamevault.evento.AvaliacaoAprovadaEvento;
import br.edu.unesc.gamevault.exception.AcessoNegadoException;
import br.edu.unesc.gamevault.exception.RecursoDuplicadoException;
import br.edu.unesc.gamevault.exception.RecursoNaoEncontradoException;
import br.edu.unesc.gamevault.exception.RegraDeNegocioException;
import br.edu.unesc.gamevault.ia.moderacao.ModeracaoService;
import br.edu.unesc.gamevault.ia.moderacao.ResultadoModeracao;
import br.edu.unesc.gamevault.mapper.AvaliacaoMapper;
import br.edu.unesc.gamevault.repository.AvaliacaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Avaliações dos jogos. Todo comentário passa pela moderação do Claude antes de ser
 * gravado (módulo 3): bloqueado não é gravado, em revisão é gravado mas não aparece
 * nem conta na nota, aprovado vai ao ar.
 *
 * <p>Criar e editar não são métodos {@code @Transactional}: a chamada à IA acontece
 * antes da escrita, para não segurar uma conexão do banco enquanto o modelo responde.
 * A escrita roda numa transação curta, aberta pelo {@link TransactionTemplate}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AvaliacaoService {
    private final AvaliacaoRepository avaliacaoRepository;
    private final UsuarioService usuarioService;
    private final JogoService jogoService;
    private final BibliotecaService bibliotecaService;
    private final AvaliacaoMapper avaliacaoMapper;
    private final ModeracaoService moderacaoService;
    private final TransactionTemplate transacao;
    private final ApplicationEventPublisher publicadorDeEventos;

    /** Listagem pública: só avaliações aprovadas. */
    @Transactional(readOnly = true)
    public Page<AvaliacaoResponse> listarPorJogo(Long jogoId, Pageable paginacao) {
        jogoService.buscarEntidade(jogoId);

        return avaliacaoRepository.findByJogo_IdAndStatusModeracao(jogoId, StatusModeracao.APROVADA, paginacao)
                .map(avaliacaoMapper::paraResposta);
    }

    /** As do próprio usuário, em qualquer situação, para ele acompanhar a moderação. */
    @Transactional(readOnly = true)
    public Page<AvaliacaoResponse> listarPorUsuario(Long usuarioId, Pageable paginacao) {
        usuarioService.buscarEntidade(usuarioId);

        return avaliacaoRepository.findByUsuario_Id(usuarioId, paginacao).map(avaliacaoMapper::paraResposta);
    }

    @Transactional(readOnly = true)
    public Page<AvaliacaoResponse> listarParaModeracao(StatusModeracao status, Pageable paginacao) {
        StatusModeracao filtro = status == null ? StatusModeracao.EM_REVISAO : status;
        return avaliacaoRepository.findByStatusModeracao(filtro, paginacao).map(avaliacaoMapper::paraResposta);
    }

    @Transactional(readOnly = true)
    public AvaliacaoResponse buscarPorId(Long id) {
        return avaliacaoMapper.paraResposta(buscarEntidade(id));
    }

    public AvaliacaoResponse criar(AvaliacaoRequest requisicao, Long usuarioId) {
        Usuario usuario = usuarioService.buscarEntidade(usuarioId);
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

        String comentario = normalizar(requisicao.comentario());
        ResultadoModeracao moderacao = moderar(jogo.getTitulo(), requisicao.nota(), comentario, usuarioId);

        AvaliacaoResponse criada = transacao.execute(status -> {
            Avaliacao avaliacao = Avaliacao.builder()
                    .usuario(usuarioService.buscarEntidade(usuarioId))
                    .jogo(jogoService.buscarEntidade(requisicao.jogoId()))
                    .nota(requisicao.nota())
                    .comentario(comentario)
                    .build();
            aplicarModeracaoDaIa(avaliacao, moderacao);

            avaliacao = avaliacaoRepository.save(avaliacao);
            recalcularEstatisticas(requisicao.jogoId());

            log.info("Avaliação criada: id={} usuario={} jogo={} nota={} moderacao={}", avaliacao.getId(),
                    usuarioId, requisicao.jogoId(), avaliacao.getNota(), avaliacao.getStatusModeracao());

            return avaliacaoMapper.paraResposta(avaliacao);
        });

        avisarSeAprovada(moderacao, requisicao.jogoId());
        return criada;
    }

    public AvaliacaoResponse atualizar(Long id, AvaliacaoAtualizacaoRequest requisicao, Long solicitanteId) {
        Avaliacao existente = buscarEntidadeComJogo(id);
        validarAutor(existente, solicitanteId);

        Long jogoId = existente.getJogo().getId();
        String comentario = normalizar(requisicao.comentario());
        ResultadoModeracao moderacao = moderar(existente.getJogo().getTitulo(), requisicao.nota(), comentario,
                solicitanteId);

        AvaliacaoResponse atualizada = transacao.execute(status -> {
            Avaliacao avaliacao = buscarEntidade(id);
            avaliacao.setNota(requisicao.nota());
            avaliacao.setComentario(comentario);
            avaliacao.setModeradaPor(null);
            aplicarModeracaoDaIa(avaliacao, moderacao);

            avaliacaoRepository.flush();
            recalcularEstatisticas(jogoId);

            log.info("Avaliação atualizada: id={} nota={} moderacao={}", id, requisicao.nota(),
                    avaliacao.getStatusModeracao());
            return avaliacaoMapper.paraResposta(avaliacao);
        });

        avisarSeAprovada(moderacao, jogoId);
        return atualizada;
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

    /** Decisão de um administrador sobre uma avaliação (aprovar ou bloquear). */
    @Transactional
    public AvaliacaoResponse revisarModeracao(Long id, ModeracaoRevisaoRequest requisicao, Long administradorId) {
        if (requisicao.decisao() == StatusModeracao.EM_REVISAO) {
            throw new RegraDeNegocioException("A revisão precisa aprovar ou bloquear a avaliação");
        }

        Avaliacao avaliacao = buscarEntidade(id);
        avaliacao.setStatusModeracao(requisicao.decisao());
        avaliacao.setMotivoModeracao(requisicao.motivo() == null || requisicao.motivo().isBlank()
                ? "Revisada por um administrador"
                : requisicao.motivo().strip());
        avaliacao.setModeradaPor(usuarioService.buscarEntidade(administradorId));
        avaliacao.setModeradaEm(LocalDateTime.now());

        avaliacaoRepository.flush();
        Long jogoId = avaliacao.getJogo().getId();
        recalcularEstatisticas(jogoId);

        if (requisicao.decisao() == StatusModeracao.APROVADA) {
            // Entregue ao módulo de resumo depois do commit
            publicadorDeEventos.publishEvent(new AvaliacaoAprovadaEvento(jogoId));
        }

        log.info("Moderação revisada: avaliacao={} decisao={} admin={}", id, requisicao.decisao(), administradorId);
        return avaliacaoMapper.paraResposta(avaliacao);
    }

    @Transactional(readOnly = true)
    public Avaliacao buscarEntidade(Long id) {
        return avaliacaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Avaliação", id));
    }

    private Avaliacao buscarEntidadeComJogo(Long id) {
        return transacao.execute(status -> {
            Avaliacao avaliacao = buscarEntidade(id);
            // Inicializa o que é usado fora da transação
            avaliacao.getJogo().getTitulo();
            avaliacao.getUsuario().getId();
            return avaliacao;
        });
    }

    private ResultadoModeracao moderar(String tituloJogo, Integer nota, String comentario, Long usuarioId) {
        ResultadoModeracao moderacao = moderacaoService.moderarComentario(tituloJogo, nota, comentario, usuarioId);

        if (moderacao.bloqueada()) {
            throw new RegraDeNegocioException("O comentário não foi publicado: " + moderacao.motivo());
        }

        return moderacao;
    }

    private void aplicarModeracaoDaIa(Avaliacao avaliacao, ResultadoModeracao moderacao) {
        avaliacao.setStatusModeracao(moderacao.decisao());
        avaliacao.setMotivoModeracao(moderacao.motivo());
        avaliacao.setContemSpoiler(moderacao.contemSpoiler());
        avaliacao.setModeradaEm(LocalDateTime.now());
    }

    private void avisarSeAprovada(ResultadoModeracao moderacao, Long jogoId) {
        if (moderacao.aprovada()) {
            publicadorDeEventos.publishEvent(new AvaliacaoAprovadaEvento(jogoId));
        }
    }

    /** RN08, agora contando só as aprovadas. */
    private void recalcularEstatisticas(Long jogoId) {
        Double media = avaliacaoRepository.calcularMediaDoJogo(jogoId, StatusModeracao.APROVADA);
        long total = avaliacaoRepository.countByJogo_IdAndStatusModeracao(jogoId, StatusModeracao.APROVADA);

        jogoService.atualizarEstatisticas(jogoId, media, total);
    }

    private void validarAutor(Avaliacao avaliacao, Long solicitanteId) {
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
