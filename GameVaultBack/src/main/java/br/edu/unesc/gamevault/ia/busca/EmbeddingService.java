package br.edu.unesc.gamevault.ia.busca;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import br.edu.unesc.gamevault.entity.Categoria;
import br.edu.unesc.gamevault.entity.Jogo;
import br.edu.unesc.gamevault.evento.JogoSalvoEvento;
import br.edu.unesc.gamevault.exception.IaIndisponivelException;
import br.edu.unesc.gamevault.ia.ml.ClienteML;
import br.edu.unesc.gamevault.repository.JogoEmbeddingRepository;
import br.edu.unesc.gamevault.repository.JogoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Mantém o embedding de cada jogo em dia. O vetor é calculado no GameVaultML a partir
 * do título, da descrição e das categorias, e só é refeito quando esse texto muda.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingService {
    private static final int TAMANHO_LOTE = 32;

    private final ClienteML clienteML;
    private final JogoRepository jogoRepository;
    private final JogoEmbeddingRepository embeddingRepository;
    private final TransactionTemplate transacao;

    /** Roda depois do commit do cadastro ou da edição, fora da thread da requisição. */
    @Async("executorIa")
    @TransactionalEventListener(fallbackExecution = true)
    public void aoSalvarJogo(JogoSalvoEvento evento) {
        try {
            atualizar(List.of(evento.jogoId()), false);
        } catch (IaIndisponivelException excecao) {
            log.warn("Embedding do jogo {} nao foi gerado agora: {}", evento.jogoId(), excecao.getMessage());
        }
    }

    /** Na subida, gera o embedding dos jogos que ainda não têm (por exemplo, os do seed). */
    @Async("executorIa")
    @EventListener(ApplicationReadyEvent.class)
    public void aoIniciar() {
        try {
            List<Long> pendentes = embeddingRepository.idsDeJogosSemEmbedding();
            if (!pendentes.isEmpty()) {
                int gerados = atualizar(pendentes, false);
                log.info("Embeddings gerados na subida: {}", gerados);
            }
        } catch (IaIndisponivelException excecao) {
            log.warn("GameVaultML indisponivel na subida; os embeddings serao gerados depois: {}",
                    excecao.getMessage());
        } catch (RuntimeException excecao) {
            log.warn("Nao foi possivel verificar os embeddings na subida: {}", excecao.getMessage());
        }
    }

    /** Recalcula os embeddings de todos os jogos (usado pelo admin). */
    public int reindexarTodos() {
        List<Long> ids = jogoRepository.findAll().stream().map(Jogo::getId).toList();
        return atualizar(ids, true);
    }

    public float[] vetorizar(String texto) {
        return clienteML.gerarEmbedding(texto);
    }

    /**
     * Gera e grava os embeddings dos jogos informados, em lotes.
     *
     * @param forcar recalcula mesmo quando o texto não mudou
     * @return quantos embeddings foram gravados
     */
    public int atualizar(List<Long> jogoIds, boolean forcar) {
        int gravados = 0;

        for (int inicio = 0; inicio < jogoIds.size(); inicio += TAMANHO_LOTE) {
            List<Long> lote = jogoIds.subList(inicio, Math.min(inicio + TAMANHO_LOTE, jogoIds.size()));
            List<TextoDoJogo> pendentes = new ArrayList<>();

            for (Long jogoId : lote) {
                Optional<TextoDoJogo> texto = carregarTexto(jogoId);
                if (texto.isEmpty()) {
                    continue;
                }
                String hashAtual = embeddingRepository.buscarHash(jogoId).orElse(null);
                if (forcar || !texto.get().hash().equals(hashAtual)) {
                    pendentes.add(texto.get());
                }
            }

            if (pendentes.isEmpty()) {
                continue;
            }

            List<float[]> vetores = clienteML.gerarEmbeddings(pendentes.stream().map(TextoDoJogo::texto).toList());
            for (int i = 0; i < pendentes.size(); i++) {
                embeddingRepository.salvar(pendentes.get(i).jogoId(), vetores.get(i), pendentes.get(i).hash());
                gravados++;
            }
        }

        if (gravados > 0) {
            log.info("{} embeddings de jogos atualizados", gravados);
        }
        return gravados;
    }

    private record TextoDoJogo(Long jogoId, String texto, String hash) {
    }

    private Optional<TextoDoJogo> carregarTexto(Long jogoId) {
        return transacao.execute(status -> jogoRepository.buscarComCategorias(jogoId)
                .map(jogo -> {
                    String texto = montarTexto(jogo);
                    return new TextoDoJogo(jogoId, texto, hash(texto));
                }));
    }

    static String montarTexto(Jogo jogo) {
        String categorias = jogo.getCategorias().stream()
                .map(Categoria::getNome)
                .sorted()
                .collect(Collectors.joining(", "));

        StringBuilder texto = new StringBuilder(jogo.getTitulo());
        if (jogo.getDescricao() != null) {
            texto.append(". ").append(jogo.getDescricao());
        }
        if (!categorias.isEmpty()) {
            texto.append(". Categorias: ").append(categorias);
        }
        return texto.toString();
    }

    private static String hash(String texto) {
        try {
            byte[] resumo = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumo);
        } catch (NoSuchAlgorithmException excecao) {
            throw new IllegalStateException(excecao);
        }
    }
}
