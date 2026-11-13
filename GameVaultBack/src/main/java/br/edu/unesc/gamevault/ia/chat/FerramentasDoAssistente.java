package br.edu.unesc.gamevault.ia.chat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Tool;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.edu.unesc.gamevault.dto.request.ListaDesejosRequest;
import br.edu.unesc.gamevault.dto.response.AcaoPendenteResponse;
import br.edu.unesc.gamevault.dto.response.CategoriaResponse;
import br.edu.unesc.gamevault.dto.response.JogoResponse;
import br.edu.unesc.gamevault.dto.response.PedidoResponse;
import br.edu.unesc.gamevault.dto.response.PromocaoResponse;
import br.edu.unesc.gamevault.entity.enums.StatusPedido;
import br.edu.unesc.gamevault.exception.AcessoNegadoException;
import br.edu.unesc.gamevault.exception.IaIndisponivelException;
import br.edu.unesc.gamevault.exception.RecursoDuplicadoException;
import br.edu.unesc.gamevault.exception.RecursoNaoEncontradoException;
import br.edu.unesc.gamevault.exception.RegraDeNegocioException;
import br.edu.unesc.gamevault.ia.busca.BuscaSemanticaService;
import br.edu.unesc.gamevault.ia.recomendacao.RecomendacaoService;
import br.edu.unesc.gamevault.ia.resumo.ResumoAvaliacoesService;
import br.edu.unesc.gamevault.service.BibliotecaService;
import br.edu.unesc.gamevault.service.JogoService;
import br.edu.unesc.gamevault.service.ListaDesejosService;
import br.edu.unesc.gamevault.service.PedidoService;
import br.edu.unesc.gamevault.service.PromocaoService;
import br.edu.unesc.gamevault.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Ferramentas que o Claude pode usar no chat. Cada uma chama um service que já existe,
 * sempre com o id do usuário que veio do JWT: o modelo não escolhe de quem são os dados,
 * então a matriz de autorização e as checagens de propriedade continuam valendo.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FerramentasDoAssistente {
    public static final String ACAO_PAGAR_PEDIDO = "PAGAR_PEDIDO";

    private final BuscaSemanticaService buscaSemanticaService;
    private final JogoService jogoService;
    private final PromocaoService promocaoService;
    private final PedidoService pedidoService;
    private final BibliotecaService bibliotecaService;
    private final UsuarioService usuarioService;
    private final ListaDesejosService listaDesejosService;
    private final RecomendacaoService recomendacaoService;
    private final ResumoAvaliacoesService resumoService;
    private final ObjectMapper objectMapper;

    /** Resultado de uma ferramenta, devolvido ao Claude como {@code tool_result}. */
    public record Resultado(String conteudo, boolean erro) {
    }

    public List<Tool> definicoes() {
        return List.of(
                ferramenta("buscar_jogos",
                        "Busca jogos ativos do catálogo pelo significado de uma descrição em linguagem natural "
                                + "(gênero, tema, modo de jogo, preço). Devolve até 5 jogos com id, título, preço e nota.",
                        Map.of("consulta", texto("O que o jogador procura, ex.: jogo de corrida barato"))),
                ferramenta("detalhes_jogo",
                        "Detalhes de um jogo: descrição, categorias, nota média, preço cheio, desconto vigente "
                                + "e preço atual com desconto.",
                        Map.of("jogoId", inteiro("Id do jogo"))),
                ferramenta("listar_promocoes_ativas",
                        "Promoções valendo agora, da maior para a menor, com preço cheio e preço com desconto.",
                        Map.of("categoria", texto("Nome de uma categoria para filtrar, ou TODAS"))),
                ferramenta("meus_pedidos",
                        "Os 10 pedidos mais recentes do jogador logado, com itens, status e valor.",
                        Map.of("status", Map.of(
                                "type", "string",
                                "enum", List.of("TODOS", "PENDENTE", "PAGO", "CANCELADO"),
                                "description", "Filtra pelo status; TODOS para não filtrar"))),
                ferramenta("minha_biblioteca",
                        "Jogos que o jogador logado possui, com horas jogadas.", Map.of()),
                ferramenta("meu_saldo",
                        "Saldo atual da carteira virtual do jogador logado.", Map.of()),
                ferramenta("minha_lista_desejos",
                        "Jogos na lista de desejos do jogador logado.", Map.of()),
                ferramenta("adicionar_lista_desejos",
                        "Adiciona um jogo à lista de desejos do jogador logado. Use só quando ele pedir.",
                        Map.of("jogoId", inteiro("Id do jogo"))),
                ferramenta("recomendar_jogos",
                        "Jogos recomendados para o jogador logado pelo modelo de recomendação da loja.", Map.of()),
                ferramenta("resumo_avaliacoes",
                        "Resumo do que os jogadores dizem de um jogo: pontos positivos e negativos.",
                        Map.of("jogoId", inteiro("Id do jogo"))),
                ferramenta("preparar_pagamento",
                        "Confere um pedido PENDENTE do jogador e coloca na tela um botão para ele confirmar o "
                                + "pagamento. NÃO paga o pedido: quem confirma é o jogador.",
                        Map.of("pedidoId", inteiro("Id do pedido pendente"))));
    }

    public Resultado executar(String nome, JsonValue entradaBruta, Long usuarioId, List<AcaoPendenteResponse> acoes) {
        try {
            JsonNode entrada = objectMapper.readTree(com.anthropic.core.ObjectMappers.jsonMapper()
                    .writeValueAsString(entradaBruta));

            Object resultado = switch (nome) {
                case "buscar_jogos" -> buscarJogos(entrada.path("consulta").asText());
                case "detalhes_jogo" -> detalhesJogo(entrada.path("jogoId").asLong());
                case "listar_promocoes_ativas" -> promocoesAtivas(entrada.path("categoria").asText("TODAS"));
                case "meus_pedidos" -> meusPedidos(usuarioId, entrada.path("status").asText("TODOS"));
                case "minha_biblioteca" -> minhaBiblioteca(usuarioId);
                case "meu_saldo" -> Map.of("saldo", usuarioService.buscarEntidade(usuarioId).getSaldo());
                case "minha_lista_desejos" -> minhaListaDeDesejos(usuarioId);
                case "adicionar_lista_desejos" -> adicionarListaDeDesejos(usuarioId, entrada.path("jogoId").asLong());
                case "recomendar_jogos" -> recomendar(usuarioId);
                case "resumo_avaliacoes" -> resumoService.buscar(entrada.path("jogoId").asLong());
                case "preparar_pagamento" -> prepararPagamento(usuarioId, entrada.path("pedidoId").asLong(), acoes);
                default -> throw new RegraDeNegocioException("Ferramenta desconhecida: " + nome);
            };

            return new Resultado(objectMapper.writeValueAsString(resultado), false);
        } catch (RecursoNaoEncontradoException | RegraDeNegocioException | AcessoNegadoException
                | RecursoDuplicadoException | IaIndisponivelException excecao) {
            return new Resultado(excecao.getMessage(), true);
        } catch (JsonProcessingException excecao) {
            log.error("Entrada ou resultado invalido na ferramenta {}", nome, excecao);
            return new Resultado("Não foi possível ler os parâmetros da ferramenta", true);
        } catch (RuntimeException excecao) {
            log.error("Erro inesperado na ferramenta {} do assistente", nome, excecao);
            return new Resultado("Erro inesperado ao consultar a loja", true);
        }
    }

    private List<Map<String, Object>> buscarJogos(String consulta) {
        return buscaSemanticaService.buscarPrimeiros(consulta, 5).stream().map(this::jogoCurto).toList();
    }

    private Map<String, Object> detalhesJogo(Long jogoId) {
        JogoResponse jogo = jogoService.buscarPorId(jogoId);
        int percentual = promocaoService.percentualVigente(jogoId, LocalDateTime.now());

        Map<String, Object> detalhes = new LinkedHashMap<>(jogoCurto(jogo));
        detalhes.put("descricao", jogo.descricao());
        detalhes.put("dataLancamento", jogo.dataLancamento());
        detalhes.put("ativo", jogo.ativo());
        detalhes.put("descontoVigentePercentual", percentual);
        detalhes.put("precoAtual", aplicarDesconto(jogo.preco(), percentual));
        return detalhes;
    }

    private List<Map<String, Object>> promocoesAtivas(String categoria) {
        List<PromocaoResponse> vigentes = promocaoService.listarVigentes();
        boolean filtrar = categoria != null && !categoria.isBlank() && !"TODAS".equalsIgnoreCase(categoria.trim());

        List<Map<String, Object>> resultado = new ArrayList<>();
        for (PromocaoResponse promocao : vigentes) {
            if (filtrar && !pertenceACategoria(promocao.jogo().id(), categoria)) {
                continue;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("jogoId", promocao.jogo().id());
            item.put("titulo", promocao.jogo().titulo());
            item.put("descontoPercentual", promocao.percentualDesconto());
            item.put("precoCheio", promocao.jogo().preco());
            item.put("precoComDesconto", aplicarDesconto(promocao.jogo().preco(), promocao.percentualDesconto()));
            item.put("terminaEm", promocao.dataFim());
            resultado.add(item);

            if (resultado.size() == 10) {
                break;
            }
        }
        return resultado;
    }

    private List<Map<String, Object>> meusPedidos(Long usuarioId, String status) {
        StatusPedido filtro = "TODOS".equalsIgnoreCase(status) ? null : StatusPedido.valueOf(status.toUpperCase());

        return pedidoService.listarDoUsuario(usuarioId, filtro,
                        PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "dataPedido")))
                .map(pedido -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("pedidoId", pedido.id());
                    item.put("data", pedido.dataPedido());
                    item.put("status", pedido.status());
                    item.put("valorTotal", pedido.valorTotal());
                    item.put("jogos", pedido.itens().stream().map(i -> i.jogo().titulo()).toList());
                    return item;
                })
                .getContent();
    }

    private List<Map<String, Object>> minhaBiblioteca(Long usuarioId) {
        return bibliotecaService.listarDoUsuario(usuarioId, PageRequest.of(0, 50))
                .map(item -> Map.<String, Object>of(
                        "jogoId", item.jogo().id(),
                        "titulo", item.jogo().titulo(),
                        "horasJogadas", item.horasJogadas()))
                .getContent();
    }

    private List<Map<String, Object>> minhaListaDeDesejos(Long usuarioId) {
        return listaDesejosService.listarDoUsuario(usuarioId, PageRequest.of(0, 50))
                .map(item -> Map.<String, Object>of(
                        "jogoId", item.jogo().id(),
                        "titulo", item.jogo().titulo(),
                        "preco", item.jogo().preco()))
                .getContent();
    }

    private Map<String, Object> adicionarListaDeDesejos(Long usuarioId, Long jogoId) {
        var adicionado = listaDesejosService.adicionar(new ListaDesejosRequest(jogoId), usuarioId);
        return Map.of("adicionado", true, "titulo", adicionado.jogo().titulo());
    }

    private List<Map<String, Object>> recomendar(Long usuarioId) {
        return recomendacaoService.listarDoUsuario(usuarioId, 5).stream()
                .map(r -> {
                    Map<String, Object> item = new LinkedHashMap<>(jogoCurto(r.jogo()));
                    item.put("motivo", r.motivo());
                    return item;
                })
                .toList();
    }

    private Map<String, Object> prepararPagamento(Long usuarioId, Long pedidoId, List<AcaoPendenteResponse> acoes) {
        PedidoResponse pedido = pedidoService.buscarPorIdDoUsuario(pedidoId, usuarioId);

        if (pedido.status() != StatusPedido.PENDENTE) {
            throw new RegraDeNegocioException(
                    "O pedido %d está %s e não pode ser pago".formatted(pedidoId, pedido.status()));
        }

        BigDecimal saldo = usuarioService.buscarEntidade(usuarioId).getSaldo();
        boolean saldoSuficiente = saldo.compareTo(pedido.valorTotal()) >= 0;

        if (saldoSuficiente) {
            acoes.add(new AcaoPendenteResponse(ACAO_PAGAR_PEDIDO, pedidoId, pedido.valorTotal(),
                    "Confirmar o pagamento do pedido #%d no valor de R$ %s".formatted(pedidoId,
                            pedido.valorTotal().setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ','))));
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("pedidoId", pedidoId);
        resultado.put("valorTotal", pedido.valorTotal());
        resultado.put("saldo", saldo);
        resultado.put("saldoSuficiente", saldoSuficiente);
        resultado.put("botaoDeConfirmacaoExibido", saldoSuficiente);
        return resultado;
    }

    private boolean pertenceACategoria(Long jogoId, String categoria) {
        String procurada = normalizar(categoria);
        return jogoService.buscarPorId(jogoId).categorias().stream()
                .map(CategoriaResponse::nome)
                .anyMatch(nome -> normalizar(nome).equals(procurada));
    }

    private Map<String, Object> jogoCurto(JogoResponse jogo) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("jogoId", jogo.id());
        item.put("titulo", jogo.titulo());
        item.put("preco", jogo.preco());
        item.put("notaMedia", jogo.notaMedia());
        item.put("totalAvaliacoes", jogo.totalAvaliacoes());
        item.put("categorias", jogo.categorias().stream().map(CategoriaResponse::nome).toList());
        return item;
    }

    private static BigDecimal aplicarDesconto(BigDecimal preco, int percentual) {
        return preco.multiply(BigDecimal.valueOf(100 - percentual))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    private static String normalizar(String texto) {
        return Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase();
    }

    private static Map<String, Object> texto(String descricao) {
        return Map.of("type", "string", "description", descricao);
    }

    private static Map<String, Object> inteiro(String descricao) {
        return Map.of("type", "integer", "description", descricao);
    }

    /** Ferramenta estrita: todos os parâmetros obrigatórios e nenhum além dos declarados. */
    private static Tool ferramenta(String nome, String descricao, Map<String, Object> parametros) {
        Tool.InputSchema.Properties.Builder propriedades = Tool.InputSchema.Properties.builder();
        parametros.forEach((chave, esquema) -> propriedades.putAdditionalProperty(chave, JsonValue.from(esquema)));

        return Tool.builder()
                .name(nome)
                .description(descricao)
                .inputSchema(Tool.InputSchema.builder()
                        .properties(propriedades.build())
                        .required(new ArrayList<>(parametros.keySet()))
                        .putAdditionalProperty("additionalProperties", JsonValue.from(false))
                        .build())
                .strict(true)
                .build();
    }
}
