package br.edu.unesc.gamevault.ia.resumo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import br.edu.unesc.gamevault.dto.response.ResumoAvaliacoesResponse;
import br.edu.unesc.gamevault.entity.Avaliacao;
import br.edu.unesc.gamevault.entity.Jogo;
import br.edu.unesc.gamevault.entity.ResumoAvaliacao;
import br.edu.unesc.gamevault.entity.enums.StatusModeracao;
import br.edu.unesc.gamevault.evento.AvaliacaoAprovadaEvento;
import br.edu.unesc.gamevault.exception.IaIndisponivelException;
import br.edu.unesc.gamevault.exception.RecursoNaoEncontradoException;
import br.edu.unesc.gamevault.exception.RegraDeNegocioException;
import br.edu.unesc.gamevault.ia.claude.ClienteClaude;
import br.edu.unesc.gamevault.ia.claude.ModuloIa;
import br.edu.unesc.gamevault.ia.claude.TextoSeguro;
import br.edu.unesc.gamevault.ia.config.IaProperties;
import br.edu.unesc.gamevault.mapper.JogoMapper;
import br.edu.unesc.gamevault.repository.AvaliacaoRepository;
import br.edu.unesc.gamevault.repository.ResumoAvaliacaoRepository;
import br.edu.unesc.gamevault.service.JogoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Resumo das avaliações (módulo 4). O Claude lê as avaliações aprovadas mais recentes
 * e escreve o resumo, os pontos positivos e negativos e o sentimento por aspecto. O
 * resultado fica guardado e só é refeito quando o jogo acumula avaliações novas.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumoAvaliacoesService {
    private static final int MAXIMO_AVALIACOES_ENVIADAS = 200;
    private static final int MAXIMO_PONTOS = 5;

    private final ClienteClaude clienteClaude;
    private final AvaliacaoRepository avaliacaoRepository;
    private final ResumoAvaliacaoRepository resumoRepository;
    private final JogoService jogoService;
    private final JogoMapper jogoMapper;
    private final IaProperties propriedades;
    private final TransactionTemplate transacao;

    /** Depois de uma avaliação aprovada, decide em segundo plano se o resumo precisa ser refeito. */
    @Async("executorIa")
    @TransactionalEventListener(fallbackExecution = true)
    public void aoAprovarAvaliacao(AvaliacaoAprovadaEvento evento) {
        try {
            if (precisaGerar(evento.jogoId())) {
                gerar(evento.jogoId());
            }
        } catch (IaIndisponivelException excecao) {
            log.info("Resumo do jogo {} nao foi atualizado agora: {}", evento.jogoId(), excecao.getMessage());
        }
    }

    public boolean precisaGerar(Long jogoId) {
        long aprovadas = avaliacaoRepository.countByJogo_IdAndStatusModeracao(jogoId, StatusModeracao.APROVADA);
        if (aprovadas < propriedades.resumo().minimoAvaliacoes()) {
            return false;
        }

        return resumoRepository.findByJogoId(jogoId)
                .map(resumo -> aprovadas - resumo.getTotalAvaliacoesBase() >= propriedades.resumo().novasParaRefazer())
                .orElse(true);
    }

    /** Gera (ou refaz) o resumo agora. */
    public ResumoAvaliacoesResponse gerar(Long jogoId) {
        AvaliacoesParaResumir material = carregarAvaliacoes(jogoId);

        if (material.avaliacoes().size() < propriedades.resumo().minimoAvaliacoes()) {
            throw new RegraDeNegocioException("O jogo precisa de pelo menos %d avaliações aprovadas para ter resumo"
                    .formatted(propriedades.resumo().minimoAvaliacoes()));
        }

        ResumoGerado gerado = clienteClaude.gerarEstruturado(ModuloIa.RESUMO_AVALIACOES,
                montarMensagem(material), ResumoGerado.class, null);

        Set<Long> idsEnviados = material.avaliacoes().stream().map(AvaliacaoResumida::id).collect(Collectors.toSet());

        ResumoAvaliacao salvo = transacao.execute(status -> {
            ResumoAvaliacao resumo = resumoRepository.findByJogoId(jogoId)
                    .orElseGet(() -> ResumoAvaliacao.builder().jogoId(jogoId).build());

            resumo.setResumo(gerado.resumo() == null ? "" : gerado.resumo().strip());
            resumo.setPontosPositivos(limitar(gerado.pontosPositivos()));
            resumo.setPontosNegativos(limitar(gerado.pontosNegativos()));
            resumo.setAspectos(gerado.aspectos() == null ? List.of() : gerado.aspectos());
            // Só ids que foram de fato enviados: o modelo não pode apontar avaliação de outro jogo
            resumo.setAvaliacoesDivergentes(gerado.avaliacoesDivergentes() == null ? List.of()
                    : gerado.avaliacoesDivergentes().stream().filter(idsEnviados::contains).distinct().toList());
            resumo.setTotalAvaliacoesBase((int) material.totalAprovadas());
            resumo.setGeradoEm(LocalDateTime.now());

            return resumoRepository.save(resumo);
        });

        log.info("Resumo das avaliacoes do jogo {} gerado com base em {} avaliacoes", jogoId,
                material.avaliacoes().size());
        return paraResposta(salvo);
    }

    @Transactional(readOnly = true)
    public ResumoAvaliacoesResponse buscar(Long jogoId) {
        jogoService.buscarEntidade(jogoId);

        return resumoRepository.findByJogoId(jogoId)
                .map(this::paraResposta)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "O jogo %d ainda não tem resumo das avaliações".formatted(jogoId)));
    }

    /** Resumos dos jogos da publicadora, para o painel dela (RN09: só os próprios jogos). */
    @Transactional(readOnly = true)
    public List<ResumoAvaliacoesResponse> listarDaPublicadora(Long publicadoraId) {
        Map<Long, Jogo> jogos = jogoService.listarEntidadesDaPublicadora(publicadoraId).stream()
                .collect(Collectors.toMap(Jogo::getId, Function.identity()));

        return resumoRepository.findByJogoIdIn(jogos.keySet()).stream()
                .map(this::paraResposta)
                .toList();
    }

    private record AvaliacaoResumida(Long id, Integer nota, String comentario) {
    }

    private record AvaliacoesParaResumir(String tituloJogo, List<AvaliacaoResumida> avaliacoes, long totalAprovadas) {
    }

    private AvaliacoesParaResumir carregarAvaliacoes(Long jogoId) {
        return transacao.execute(status -> {
            Jogo jogo = jogoService.buscarEntidade(jogoId);
            PageRequest maisRecentes = PageRequest.of(0, MAXIMO_AVALIACOES_ENVIADAS,
                    Sort.by(Sort.Direction.DESC, "dataAvaliacao"));

            List<AvaliacaoResumida> avaliacoes = avaliacaoRepository
                    .findByJogo_IdAndStatusModeracao(jogoId, StatusModeracao.APROVADA, maisRecentes)
                    .map(this::resumir)
                    .getContent();
            long total = avaliacaoRepository.countByJogo_IdAndStatusModeracao(jogoId, StatusModeracao.APROVADA);

            return new AvaliacoesParaResumir(jogo.getTitulo(), avaliacoes, total);
        });
    }

    private AvaliacaoResumida resumir(Avaliacao avaliacao) {
        return new AvaliacaoResumida(avaliacao.getId(), avaliacao.getNota(), avaliacao.getComentario());
    }

    private String montarMensagem(AvaliacoesParaResumir material) {
        StringBuilder mensagem = new StringBuilder(TextoSeguro.tag("jogo", material.tituloJogo())).append("\n");

        for (AvaliacaoResumida avaliacao : material.avaliacoes()) {
            mensagem.append("<avaliacao id=\"").append(avaliacao.id())
                    .append("\" nota=\"").append(avaliacao.nota()).append("\">")
                    .append(TextoSeguro.escapar(avaliacao.comentario() == null ? "(sem comentário)" : avaliacao.comentario()))
                    .append("</avaliacao>\n");
        }

        return mensagem.toString();
    }

    private static List<String> limitar(List<String> pontos) {
        if (pontos == null) {
            return List.of();
        }
        return pontos.stream().filter(p -> p != null && !p.isBlank()).map(String::strip).limit(MAXIMO_PONTOS).toList();
    }

    private ResumoAvaliacoesResponse paraResposta(ResumoAvaliacao resumo) {
        return transacao.execute(status -> {
            Jogo jogo = jogoService.buscarEntidade(resumo.getJogoId());
            return new ResumoAvaliacoesResponse(
                    jogoMapper.paraResumo(jogo),
                    resumo.getResumo(),
                    resumo.getPontosPositivos(),
                    resumo.getPontosNegativos(),
                    resumo.getAspectos(),
                    resumo.getAvaliacoesDivergentes(),
                    resumo.getTotalAvaliacoesBase(),
                    resumo.getGeradoEm());
        });
    }
}
