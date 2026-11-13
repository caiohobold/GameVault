package br.edu.unesc.gamevault.ia.cadastro;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import br.edu.unesc.gamevault.dto.request.CategoriasJogoRequest;
import br.edu.unesc.gamevault.dto.request.DescricaoJogoRequest;
import br.edu.unesc.gamevault.dto.request.DuplicadosRequest;
import br.edu.unesc.gamevault.dto.request.TraducaoRequest;
import br.edu.unesc.gamevault.dto.response.CategoriasSugeridasResponse;
import br.edu.unesc.gamevault.dto.response.DescricaoGeradaResponse;
import br.edu.unesc.gamevault.dto.response.JogoDuplicadoResponse;
import br.edu.unesc.gamevault.dto.response.JogoResponse;
import br.edu.unesc.gamevault.dto.response.JogoResumoResponse;
import br.edu.unesc.gamevault.dto.response.TraducaoResponse;
import br.edu.unesc.gamevault.entity.Categoria;
import br.edu.unesc.gamevault.exception.RegraDeNegocioException;
import br.edu.unesc.gamevault.ia.LimiteDeUsoService;
import br.edu.unesc.gamevault.ia.busca.EmbeddingService;
import br.edu.unesc.gamevault.ia.claude.ClienteClaude;
import br.edu.unesc.gamevault.ia.claude.ModuloIa;
import br.edu.unesc.gamevault.ia.claude.TextoSeguro;
import br.edu.unesc.gamevault.ia.moderacao.ModeracaoService;
import br.edu.unesc.gamevault.ia.moderacao.ResultadoModeracao;
import br.edu.unesc.gamevault.mapper.CategoriaMapper;
import br.edu.unesc.gamevault.repository.CategoriaRepository;
import br.edu.unesc.gamevault.repository.JogoEmbeddingRepository;
import br.edu.unesc.gamevault.repository.JogoEmbeddingRepository.JogoSimilaridade;
import br.edu.unesc.gamevault.service.JogoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Assistente de cadastro para publicadoras (módulo 5). Tudo aqui é sugestão: o jogo
 * continua sendo gravado pelo POST/PUT /jogos de sempre, com as validações de sempre.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssistenteCadastroService {
    private static final int LIMITE_DESCRICAO = 2000;
    private static final int MAXIMO_CATEGORIAS = 4;
    private static final double SIMILARIDADE_DE_DUPLICIDADE = 0.9;

    private final ClienteClaude clienteClaude;
    private final ModeracaoService moderacaoService;
    private final LimiteDeUsoService limiteDeUso;
    private final CategoriaRepository categoriaRepository;
    private final CategoriaMapper categoriaMapper;
    private final EmbeddingService embeddingService;
    private final JogoEmbeddingRepository embeddingRepository;
    private final JogoService jogoService;

    public DescricaoGeradaResponse gerarDescricao(DescricaoJogoRequest requisicao, Long usuarioId) {
        limiteDeUso.registrarChamada(usuarioId, LimiteDeUsoService.ASSISTENTE_CADASTRO);

        String mensagem = TextoSeguro.tag("titulo", requisicao.titulo()) + "\n"
                + TextoSeguro.tag("topicos", requisicao.topicos());

        String descricao = gerarTexto(mensagem, usuarioId);
        if (descricao.length() > LIMITE_DESCRICAO) {
            // Uma segunda tentativa pedindo explicitamente um texto mais curto
            descricao = gerarTexto(mensagem + "\n" + TextoSeguro.tag("observacao",
                    "A versão anterior passou de 2000 caracteres. Escreva uma versão com no máximo 1200."),
                    usuarioId);
        }
        if (descricao.length() > LIMITE_DESCRICAO) {
            throw new RegraDeNegocioException(
                    "A descrição gerada ficou longa demais. Tente com menos tópicos.");
        }

        ResultadoModeracao moderacao = moderacaoService.moderarTexto(requisicao.titulo(), descricao, usuarioId);
        if (moderacao.bloqueada()) {
            throw new RegraDeNegocioException("A descrição gerada não passou na moderação: " + moderacao.motivo());
        }

        log.info("Descricao gerada para '{}' ({} caracteres)", requisicao.titulo(), descricao.length());
        return new DescricaoGeradaResponse(descricao, descricao.length());
    }

    public CategoriasSugeridasResponse sugerirCategorias(CategoriasJogoRequest requisicao, Long usuarioId) {
        limiteDeUso.registrarChamada(usuarioId, LimiteDeUsoService.ASSISTENTE_CADASTRO);

        List<Categoria> categorias = categoriaRepository.findAll();
        String lista = categorias.stream()
                .map(c -> c.getId() + " - " + c.getNome() + " - " + (c.getDescricao() == null ? "" : c.getDescricao()))
                .collect(Collectors.joining("\n"));

        String mensagem = TextoSeguro.tag("categorias_disponiveis", lista) + "\n"
                + TextoSeguro.tag("titulo", requisicao.titulo()) + "\n"
                + TextoSeguro.tag("descricao", requisicao.descricao() == null ? "" : requisicao.descricao());

        SugestaoCategorias sugestao = clienteClaude.gerarEstruturado(ModuloIa.CATEGORIAS_JOGO, mensagem,
                SugestaoCategorias.class, usuarioId);

        List<Categoria> escolhidas = filtrarExistentes(sugestao.categoriaIds(), categorias);
        return new CategoriasSugeridasResponse(
                escolhidas.stream().map(categoriaMapper::paraResposta).toList(),
                sugestao.justificativa());
    }

    public TraducaoResponse traduzir(TraducaoRequest requisicao, Long usuarioId) {
        limiteDeUso.registrarChamada(usuarioId, LimiteDeUsoService.ASSISTENTE_CADASTRO);

        String mensagem = TextoSeguro.tag("idioma", requisicao.idioma().codigo()) + "\n"
                + TextoSeguro.tag("texto", requisicao.texto());

        TraducaoGerada traducao = clienteClaude.gerarEstruturado(ModuloIa.TRADUCAO, mensagem, TraducaoGerada.class,
                usuarioId);
        return new TraducaoResponse(requisicao.idioma(), traducao.texto() == null ? "" : traducao.texto().strip());
    }

    /** Jogos ativos muito parecidos com o que está sendo cadastrado (embeddings). */
    public List<JogoDuplicadoResponse> procurarDuplicados(DuplicadosRequest requisicao, Long usuarioId) {
        limiteDeUso.registrarChamada(usuarioId, LimiteDeUsoService.ASSISTENTE_CADASTRO);

        String texto = requisicao.titulo()
                + (requisicao.descricao() == null || requisicao.descricao().isBlank() ? "" : ". " + requisicao.descricao());
        float[] vetor = embeddingService.vetorizar(texto);

        List<JogoSimilaridade> parecidos = embeddingRepository.semelhantesAoVetor(vetor, SIMILARIDADE_DE_DUPLICIDADE,
                requisicao.ignorarJogoId(), 5);
        Map<Long, Double> similaridades = parecidos.stream()
                .collect(Collectors.toMap(JogoSimilaridade::jogoId, JogoSimilaridade::similaridade, (a, b) -> a));

        return jogoService.buscarRespostasPorIds(parecidos.stream().map(JogoSimilaridade::jogoId).toList()).stream()
                .map(jogo -> new JogoDuplicadoResponse(resumo(jogo), similaridades.get(jogo.id())))
                .toList();
    }

    private String gerarTexto(String mensagem, Long usuarioId) {
        DescricaoGerada gerada = clienteClaude.gerarEstruturado(ModuloIa.DESCRICAO_JOGO, mensagem,
                DescricaoGerada.class, usuarioId);
        return gerada.descricao() == null ? "" : gerada.descricao().strip();
    }

    /** Só ids que existem no banco, sem repetição, no máximo 4. */
    static List<Categoria> filtrarExistentes(List<Long> ids, List<Categoria> categorias) {
        if (ids == null) {
            return List.of();
        }

        Map<Long, Categoria> porId = categorias.stream()
                .collect(Collectors.toMap(Categoria::getId, Function.identity()));

        return new LinkedHashSet<>(ids).stream()
                .map(porId::get)
                .filter(categoria -> categoria != null)
                .limit(MAXIMO_CATEGORIAS)
                .toList();
    }

    private static JogoResumoResponse resumo(JogoResponse jogo) {
        return new JogoResumoResponse(jogo.id(), jogo.titulo(), jogo.preco(), jogo.notaMedia(), jogo.ativo());
    }
}
