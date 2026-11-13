package br.edu.unesc.gamevault.ia.busca;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import br.edu.unesc.gamevault.dto.response.BuscaSemanticaResponse;
import br.edu.unesc.gamevault.dto.response.BuscaSemanticaResponse.FiltrosAplicados;
import br.edu.unesc.gamevault.dto.response.JogoResponse;
import br.edu.unesc.gamevault.entity.Categoria;
import br.edu.unesc.gamevault.exception.IaIndisponivelException;
import br.edu.unesc.gamevault.exception.RegraDeNegocioException;
import br.edu.unesc.gamevault.ia.claude.ClienteClaude;
import br.edu.unesc.gamevault.ia.claude.ModuloIa;
import br.edu.unesc.gamevault.ia.claude.TextoSeguro;
import br.edu.unesc.gamevault.mapper.CategoriaMapper;
import br.edu.unesc.gamevault.repository.CategoriaRepository;
import br.edu.unesc.gamevault.repository.JogoEmbeddingRepository;
import br.edu.unesc.gamevault.repository.JogoEmbeddingRepository.FiltrosSql;
import br.edu.unesc.gamevault.repository.JogoEmbeddingRepository.JogoSimilaridade;
import br.edu.unesc.gamevault.service.JogoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Busca em linguagem natural (módulo 2). O Claude separa a frase em texto + filtros,
 * o GameVaultML transforma o texto em vetor e o pgvector ordena os jogos pelo
 * significado. Os filtros de preço, categoria e promoção são aplicados no SQL.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BuscaSemanticaService {
    private static final int TAMANHO_MAXIMO_CONSULTA = 300;

    private final ClienteClaude clienteClaude;
    private final EmbeddingService embeddingService;
    private final JogoEmbeddingRepository embeddingRepository;
    private final CategoriaRepository categoriaRepository;
    private final CategoriaMapper categoriaMapper;
    private final JogoService jogoService;

    public BuscaSemanticaResponse buscar(String consulta, Pageable paginacao) {
        String frase = validar(consulta);
        List<Categoria> categorias = categoriaRepository.findAll();

        FiltrosBusca filtros = interpretar(frase, categorias);
        boolean interpretadoPorIa = filtros != null;
        if (filtros == null) {
            filtros = new FiltrosBusca(frase, 0, List.of(), false);
        }

        List<Categoria> categoriasEscolhidas = resolverCategorias(filtros.categorias(), categorias);
        BigDecimal precoMaximo = filtros.precoMaximo() > 0
                ? BigDecimal.valueOf(filtros.precoMaximo()).setScale(2, RoundingMode.HALF_UP)
                : null;
        String textoSemantico = filtros.textoSemantico() == null || filtros.textoSemantico().isBlank()
                ? frase
                : filtros.textoSemantico();

        FiltrosAplicados aplicados = new FiltrosAplicados(
                textoSemantico,
                precoMaximo,
                categoriasEscolhidas.stream().map(categoriaMapper::paraResposta).toList(),
                filtros.apenasEmPromocao(),
                interpretadoPorIa);

        float[] vetor;
        try {
            vetor = embeddingService.vetorizar(textoSemantico);
        } catch (IaIndisponivelException excecao) {
            log.warn("Busca semantica indisponivel, usando busca por titulo: {}", excecao.getMessage());
            return new BuscaSemanticaResponse(frase, "TEXTUAL", aplicados, jogoService.listar(frase, null, paginacao));
        }

        FiltrosSql filtrosSql = new FiltrosSql(precoMaximo,
                categoriasEscolhidas.stream().map(Categoria::getId).collect(Collectors.toSet()),
                filtros.apenasEmPromocao());

        List<JogoSimilaridade> encontrados = embeddingRepository.buscarPorSimilaridade(vetor, filtrosSql,
                paginacao.getPageSize(), paginacao.getOffset());
        long total = embeddingRepository.contarComFiltros(filtrosSql);

        List<JogoResponse> jogos = jogoService.buscarRespostasPorIds(
                encontrados.stream().map(JogoSimilaridade::jogoId).toList());
        Page<JogoResponse> pagina = new PageImpl<>(jogos, paginacao, total);

        log.info("Busca semantica '{}' -> {} resultados (ia={})", frase, total, interpretadoPorIa);
        return new BuscaSemanticaResponse(frase, "SEMANTICA", aplicados, pagina);
    }

    /** Os primeiros jogos da busca, para o chat. */
    public List<JogoResponse> buscarPrimeiros(String consulta, int quantidade) {
        return buscar(consulta, Pageable.ofSize(quantidade)).resultados().getContent();
    }

    private FiltrosBusca interpretar(String frase, List<Categoria> categorias) {
        String listaDeCategorias = categorias.stream()
                .map(Categoria::getNome)
                .sorted()
                .collect(Collectors.joining("\n"));

        String mensagem = TextoSeguro.tag("categorias_disponiveis", listaDeCategorias) + "\n"
                + TextoSeguro.tag("busca", frase);

        try {
            return clienteClaude.gerarEstruturado(ModuloIa.FILTROS_BUSCA, mensagem, FiltrosBusca.class, null);
        } catch (IaIndisponivelException excecao) {
            log.debug("Filtros da busca sem IA: {}", excecao.getMessage());
            return null;
        }
    }

    /** Aceita só categorias que existem no banco; o resto é descartado. */
    static List<Categoria> resolverCategorias(List<String> nomes, List<Categoria> categorias) {
        if (nomes == null || nomes.isEmpty()) {
            return List.of();
        }

        Map<String, Categoria> porNome = categorias.stream()
                .collect(Collectors.toMap(c -> normalizar(c.getNome()), Function.identity(), (a, b) -> a));

        Set<Categoria> escolhidas = new LinkedHashSet<>();
        for (String nome : nomes) {
            Categoria categoria = porNome.get(normalizar(nome));
            if (categoria != null) {
                escolhidas.add(categoria);
            }
        }
        return List.copyOf(escolhidas);
    }

    private static String normalizar(String texto) {
        String semAcento = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcento.trim().toLowerCase();
    }

    private String validar(String consulta) {
        if (consulta == null || consulta.isBlank()) {
            throw new RegraDeNegocioException("Digite o que você procura na busca");
        }
        String frase = consulta.trim();
        if (frase.length() > TAMANHO_MAXIMO_CONSULTA) {
            throw new RegraDeNegocioException(
                    "A busca pode ter no máximo %d caracteres".formatted(TAMANHO_MAXIMO_CONSULTA));
        }
        return frase;
    }
}
