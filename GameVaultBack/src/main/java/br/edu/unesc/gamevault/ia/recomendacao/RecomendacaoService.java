package br.edu.unesc.gamevault.ia.recomendacao;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import br.edu.unesc.gamevault.dto.response.JogoResponse;
import br.edu.unesc.gamevault.dto.response.RecomendacaoResponse;
import br.edu.unesc.gamevault.dto.response.TreinoRecomendacaoResponse;
import br.edu.unesc.gamevault.entity.JogoSimilar;
import br.edu.unesc.gamevault.entity.RecomendacaoUsuario;
import br.edu.unesc.gamevault.entity.VisualizacaoJogo;
import br.edu.unesc.gamevault.exception.IaIndisponivelException;
import br.edu.unesc.gamevault.ia.MetricaModeloService;
import br.edu.unesc.gamevault.ia.ml.ClienteML;
import br.edu.unesc.gamevault.ia.ml.ContratosMl.TreinoRecomendacaoResposta;
import br.edu.unesc.gamevault.repository.BibliotecaRepository;
import br.edu.unesc.gamevault.repository.JogoEmbeddingRepository;
import br.edu.unesc.gamevault.repository.JogoEmbeddingRepository.JogoSimilaridade;
import br.edu.unesc.gamevault.repository.JogoSimilarRepository;
import br.edu.unesc.gamevault.repository.ListaDesejosRepository;
import br.edu.unesc.gamevault.repository.RecomendacaoUsuarioRepository;
import br.edu.unesc.gamevault.repository.VisualizacaoJogoRepository;
import br.edu.unesc.gamevault.service.JogoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Recomendação de jogos (módulo 1). O modelo de filtragem colaborativa é treinado no
 * GameVaultML; aqui o resultado é gravado e servido, sempre conferindo na hora as
 * regras que podem ter mudado desde o treino: jogo já comprado (RN01) e jogo inativo
 * (RN11) nunca são recomendados.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecomendacaoService {
    public static final String ORIGEM_COLABORATIVA = "FILTRAGEM_COLABORATIVA";
    public static final String ORIGEM_CONTEUDO = "CONTEUDO";

    private final ClienteML clienteML;
    private final RecomendacaoUsuarioRepository recomendacaoRepository;
    private final JogoSimilarRepository jogoSimilarRepository;
    private final VisualizacaoJogoRepository visualizacaoRepository;
    private final BibliotecaRepository bibliotecaRepository;
    private final ListaDesejosRepository listaDesejosRepository;
    private final JogoEmbeddingRepository embeddingRepository;
    private final JogoService jogoService;
    private final MetricaModeloService metricaService;
    private final TransactionTemplate transacao;

    /** Treina o modelo no GameVaultML e substitui as recomendações gravadas. */
    public TreinoRecomendacaoResponse treinar() {
        TreinoRecomendacaoResposta resultado = clienteML.treinarRecomendacoes();
        LocalDateTime agora = LocalDateTime.now();

        transacao.executeWithoutResult(status -> {
            recomendacaoRepository.deleteAllInBatch();
            jogoSimilarRepository.deleteAllInBatch();

            recomendacaoRepository.saveAll(resultado.recomendacoes().stream()
                    .map(r -> RecomendacaoUsuario.builder()
                            .usuarioId(r.usuarioId())
                            .jogoId(r.jogoId())
                            .pontuacao(r.pontuacao())
                            .motivoJogoId(r.motivoJogoId())
                            .geradaEm(agora)
                            .build())
                    .toList());

            jogoSimilarRepository.saveAll(resultado.similares().stream()
                    .map(s -> JogoSimilar.builder()
                            .jogoId(s.jogoId())
                            .similarId(s.similarId())
                            .pontuacao(s.pontuacao())
                            .build())
                    .toList());
        });

        var metricas = resultado.metricas();
        metricaService.registrar(MetricaModeloService.MODELO_RECOMENDACAO, MetricaModeloService.METRICA_HIT_RATE_10,
                metricas.hitRate10(), metricas.usuariosAvaliados());
        metricaService.registrar(MetricaModeloService.MODELO_RECOMENDACAO,
                MetricaModeloService.METRICA_HIT_RATE_10_POPULARIDADE, metricas.hitRate10Popularidade(),
                metricas.usuariosAvaliados());

        log.info("Recomendacoes treinadas: {} recomendacoes, {} pares similares, hit rate@10={} (popularidade={})",
                resultado.recomendacoes().size(), resultado.similares().size(), metricas.hitRate10(),
                metricas.hitRate10Popularidade());

        return new TreinoRecomendacaoResponse(resultado.recomendacoes().size(), resultado.similares().size(),
                metricas.hitRate10(), metricas.hitRate10Popularidade(), metricas.usuariosAvaliados());
    }

    @Scheduled(cron = "${gamevault.ia.recomendacao.cron}")
    public void treinarAgendado() {
        try {
            treinar();
        } catch (IaIndisponivelException excecao) {
            log.warn("Treino agendado da recomendacao nao rodou: {}", excecao.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public List<RecomendacaoResponse> listarDoUsuario(Long usuarioId, int limiteSolicitado) {
        int limite = limitar(limiteSolicitado);
        Set<Long> possuidos = new HashSet<>(bibliotecaRepository.buscarIdsDeJogosDoUsuario(usuarioId));

        List<RecomendacaoUsuario> recomendacoes = recomendacaoRepository.findByUsuarioIdOrderByPontuacaoDesc(usuarioId)
                .stream()
                .filter(r -> !possuidos.contains(r.getJogoId()))
                .toList();

        if (!recomendacoes.isEmpty()) {
            return montarColaborativas(recomendacoes, limite);
        }

        return recomendarPorConteudo(usuarioId, possuidos, limite);
    }

    @Transactional(readOnly = true)
    public List<RecomendacaoResponse> listarSimilares(Long jogoId, int limiteSolicitado) {
        int limite = limitar(limiteSolicitado);
        jogoService.buscarEntidade(jogoId);

        List<JogoSimilar> similares = jogoSimilarRepository.findByJogoIdOrderByPontuacaoDesc(jogoId);
        if (!similares.isEmpty()) {
            Map<Long, Double> pontuacoes = similares.stream()
                    .collect(Collectors.toMap(JogoSimilar::getSimilarId, JogoSimilar::getPontuacao, (a, b) -> a));
            return ativos(similares.stream().map(JogoSimilar::getSimilarId).toList(), limite).stream()
                    .map(jogo -> new RecomendacaoResponse(jogo, pontuacoes.get(jogo.id()),
                            "Quem tem este jogo também costuma ter este", ORIGEM_COLABORATIVA))
                    .toList();
        }

        // Antes do primeiro treino, usa só a semelhança de texto
        List<JogoSimilaridade> vizinhos = embeddingRepository.vizinhosDoJogo(jogoId, limite);
        return comPontuacaoDeConteudo(vizinhos, "Parecido com este jogo", limite);
    }

    /**
     * Grava que o usuário abriu a página do jogo. Alimenta a recomendação de quem ainda
     * não comprou nada. Roda em segundo plano, então uma falha aqui nunca chega à
     * página do jogo.
     */
    @Async("executorIa")
    @Transactional
    public void registrarVisualizacao(Long usuarioId, Long jogoId) {
        VisualizacaoJogo visualizacao = visualizacaoRepository.findByUsuarioIdAndJogoId(usuarioId, jogoId)
                .orElseGet(() -> VisualizacaoJogo.builder().usuarioId(usuarioId).jogoId(jogoId).build());
        visualizacao.setVisualizadoEm(LocalDateTime.now());
        visualizacaoRepository.save(visualizacao);
    }

    private List<RecomendacaoResponse> montarColaborativas(List<RecomendacaoUsuario> recomendacoes, int limite) {
        Map<Long, RecomendacaoUsuario> porJogo = recomendacoes.stream()
                .collect(Collectors.toMap(RecomendacaoUsuario::getJogoId, Function.identity(), (a, b) -> a));

        List<JogoResponse> jogos = ativos(recomendacoes.stream().map(RecomendacaoUsuario::getJogoId).toList(), limite);

        Set<Long> idsDosMotivos = jogos.stream()
                .map(j -> porJogo.get(j.id()).getMotivoJogoId())
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        Map<Long, String> titulos = jogoService.buscarRespostasPorIds(new ArrayList<>(idsDosMotivos)).stream()
                .collect(Collectors.toMap(JogoResponse::id, JogoResponse::titulo));

        return jogos.stream()
                .map(jogo -> {
                    RecomendacaoUsuario recomendacao = porJogo.get(jogo.id());
                    String titulo = titulos.get(recomendacao.getMotivoJogoId());
                    String motivo = titulo != null
                            ? "Porque você jogou " + titulo
                            : "Jogadores com gosto parecido com o seu gostaram";
                    return new RecomendacaoResponse(jogo, recomendacao.getPontuacao(), motivo, ORIGEM_COLABORATIVA);
                })
                .toList();
    }

    /**
     * Usuário sem histórico de compra: usa o texto dos jogos que ele favoritou ou visitou
     * para achar jogos parecidos.
     */
    private List<RecomendacaoResponse> recomendarPorConteudo(Long usuarioId, Set<Long> possuidos, int limite) {
        Set<Long> referencias = new LinkedHashSet<>(listaDesejosRepository.buscarIdsDeJogosDoUsuario(usuarioId));
        visualizacaoRepository.findTop10ByUsuarioIdOrderByVisualizadoEmDesc(usuarioId)
                .forEach(v -> referencias.add(v.getJogoId()));

        if (referencias.isEmpty()) {
            return List.of();
        }

        Set<Long> excluir = new HashSet<>(possuidos);
        excluir.addAll(listaDesejosRepository.buscarIdsDeJogosDoUsuario(usuarioId));

        List<JogoSimilaridade> vizinhos = embeddingRepository.vizinhosDoCentro(referencias, excluir, limite);
        return comPontuacaoDeConteudo(vizinhos, "Parecido com jogos que você favoritou ou visitou", limite);
    }

    private List<RecomendacaoResponse> comPontuacaoDeConteudo(List<JogoSimilaridade> vizinhos, String motivo,
            int limite) {
        Map<Long, Double> pontuacoes = vizinhos.stream()
                .collect(Collectors.toMap(JogoSimilaridade::jogoId, JogoSimilaridade::similaridade, (a, b) -> a));

        return ativos(vizinhos.stream().map(JogoSimilaridade::jogoId).toList(), limite).stream()
                .map(jogo -> new RecomendacaoResponse(jogo, pontuacoes.get(jogo.id()), motivo, ORIGEM_CONTEUDO))
                .toList();
    }

    private static int limitar(int limite) {
        return Math.max(1, Math.min(limite, 20));
    }

    private List<JogoResponse> ativos(List<Long> ids, int limite) {
        return jogoService.buscarRespostasPorIds(ids).stream()
                .filter(jogo -> Boolean.TRUE.equals(jogo.ativo()))
                .limit(limite)
                .toList();
    }
}
