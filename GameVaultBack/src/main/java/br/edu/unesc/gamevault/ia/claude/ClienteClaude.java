package br.edu.unesc.gamevault.ia.claude;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

import com.anthropic.client.AnthropicClient;
import com.anthropic.core.JsonSchemaLocalValidation;
import com.anthropic.core.JsonValue;
import com.anthropic.errors.AnthropicException;
import com.anthropic.errors.AnthropicServiceException;
import com.anthropic.errors.PermissionDeniedException;
import com.anthropic.errors.RateLimitException;
import com.anthropic.errors.UnauthorizedException;
import com.anthropic.models.messages.CacheControlEphemeral;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.StructuredMessage;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.StructuredOutputConfig;
import com.anthropic.models.messages.TextBlockParam;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.Usage;

import br.edu.unesc.gamevault.exception.IaIndisponivelException;
import br.edu.unesc.gamevault.ia.config.IaProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Única classe que conversa com a API do Claude. Os services dos módulos dependem
 * dela, e não do SDK, o que permite trocá-la por um mock nos testes.
 *
 * <p>Toda chamada: usa o modelo e o esforço do módulo, manda o system prompt com
 * cache, habilita o fallback do servidor para recusas, confere o {@code stop_reason}
 * antes de ler a resposta e registra o uso em {@code ia_uso}. Qualquer falha vira
 * {@link IaIndisponivelException}, que cada módulo trata do seu jeito.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClienteClaude {
    private static final long MAX_TOKENS = 16000L;
    private static final String BETA_FALLBACK = "server-side-fallback-2026-07-01";

    private final AnthropicClient cliente;
    private final IaProperties propriedades;
    private final CarregadorDePrompts prompts;
    private final RegistroDeUsoIa registroDeUso;

    public boolean disponivel() {
        return propriedades.habilitada() && propriedades.chaveConfigurada();
    }

    /**
     * Chamada com saída estruturada: o Claude devolve um JSON que segue o esquema do
     * record {@code tipo}, e o SDK já entrega o objeto pronto.
     */
    public <T> T gerarEstruturado(ModuloIa modulo, String mensagem, Class<T> tipo, Long usuarioId) {
        garantirDisponivel(modulo);

        StructuredMessageCreateParams<T> parametros = parametrosBase(modulo)
                .addUserMessage(mensagem)
                .outputConfig(StructuredOutputConfig.<T>builder()
                        .effort(modulo.esforco())
                        .format(tipo, JsonSchemaLocalValidation.YES)
                        .build())
                .build();

        StructuredMessage<T> resposta = executar(modulo, usuarioId,
                () -> cliente.messages().create(parametros),
                StructuredMessage::usage, StructuredMessage::stopReason);

        try {
            return resposta.content().stream()
                    .flatMap(bloco -> bloco.text().stream())
                    .map(texto -> texto.text())
                    .findFirst()
                    .orElseThrow(() -> new IaIndisponivelException("A IA não devolveu conteúdo"));
        } catch (RuntimeException excecao) {
            if (excecao instanceof IaIndisponivelException indisponivel) {
                throw indisponivel;
            }
            log.error("Resposta estruturada invalida do modulo {}", modulo, excecao);
            throw new IaIndisponivelException("A IA devolveu uma resposta em formato inesperado", excecao);
        }
    }

    /**
     * Um turno de conversa com ferramentas (usado pelo chat). Quem chama mantém o
     * histórico e executa as ferramentas pedidas.
     */
    public Message conversar(ModuloIa modulo, List<MessageParam> historico, List<Tool> ferramentas,
            Long usuarioId) {
        garantirDisponivel(modulo);

        MessageCreateParams.Builder construtor = parametrosBase(modulo)
                .messages(historico)
                .outputConfig(OutputConfig.builder().effort(modulo.esforco()).build());
        ferramentas.forEach(construtor::addTool);
        MessageCreateParams parametros = construtor.build();

        return executar(modulo, usuarioId, () -> cliente.messages().create(parametros),
                Message::usage, Message::stopReason);
    }

    private MessageCreateParams.Builder parametrosBase(ModuloIa modulo) {
        return MessageCreateParams.builder()
                .model(propriedades.modeloDe(modulo))
                .maxTokens(MAX_TOKENS)
                // System prompt estável e com cache: nada que muda a cada chamada entra aqui
                .systemOfTextBlockParams(List.of(TextBlockParam.builder()
                        .text(prompts.de(modulo))
                        .cacheControl(CacheControlEphemeral.builder().build())
                        .build()))
                // Se o modelo recusar por engano, a Anthropic tenta de novo com outro modelo
                .putAdditionalHeader("anthropic-beta", BETA_FALLBACK)
                .putAdditionalBodyProperty("fallbacks", JsonValue.from("default"));
    }

    private <R> R executar(ModuloIa modulo, Long usuarioId, Supplier<R> chamada,
            java.util.function.Function<R, Usage> usoDe,
            java.util.function.Function<R, Optional<StopReason>> paradaDe) {
        String modelo = propriedades.modeloDe(modulo);
        long inicio = System.currentTimeMillis();
        R resposta;

        try {
            resposta = chamada.get();
        } catch (RateLimitException excecao) {
            falhar(modulo, modelo, usuarioId, inicio, "limite de requisicoes da API", excecao);
            throw new IaIndisponivelException("A IA está recebendo muitas requisições. Tente de novo em instantes.",
                    excecao);
        } catch (UnauthorizedException | PermissionDeniedException excecao) {
            log.error("Chave da API da Anthropic invalida ou sem permissao (modulo {})", modulo);
            falhar(modulo, modelo, usuarioId, inicio, "chave invalida", excecao);
            throw new IaIndisponivelException("A integração com a IA não está configurada corretamente.", excecao);
        } catch (AnthropicServiceException excecao) {
            log.error("API do Claude respondeu {} no modulo {}", excecao.statusCode(), modulo, excecao);
            falhar(modulo, modelo, usuarioId, inicio, "status " + excecao.statusCode(), excecao);
            throw new IaIndisponivelException("A IA está indisponível no momento.", excecao);
        } catch (AnthropicException excecao) {
            log.warn("Falha de comunicacao com a API do Claude no modulo {}: {}", modulo, excecao.getMessage());
            falhar(modulo, modelo, usuarioId, inicio, excecao.getMessage(), excecao);
            throw new IaIndisponivelException("Não foi possível falar com a IA no momento.", excecao);
        }

        Usage uso = usoDe.apply(resposta);
        long duracao = System.currentTimeMillis() - inicio;
        StopReason parada = paradaDe.apply(resposta).orElse(null);

        if (StopReason.REFUSAL.equals(parada)) {
            registrar(modulo, modelo, usuarioId, uso, duracao, false, "recusa do modelo");
            throw new IaIndisponivelException("A IA não pôde processar este conteúdo.");
        }
        if (StopReason.MAX_TOKENS.equals(parada)) {
            registrar(modulo, modelo, usuarioId, uso, duracao, false, "resposta cortada por max_tokens");
            throw new IaIndisponivelException("A resposta da IA ficou incompleta.");
        }

        registrar(modulo, modelo, usuarioId, uso, duracao, true, null);
        log.debug("Claude respondeu no modulo {} em {} ms (entrada={} saida={})", modulo, duracao,
                uso.inputTokens(), uso.outputTokens());
        return resposta;
    }

    private void garantirDisponivel(ModuloIa modulo) {
        if (!disponivel()) {
            log.debug("Modulo {} chamado sem a IA configurada", modulo);
            throw new IaIndisponivelException(
                    "A IA não está configurada neste ambiente (defina ANTHROPIC_API_KEY).");
        }
    }

    private void falhar(ModuloIa modulo, String modelo, Long usuarioId, long inicio, String erro,
            Exception excecao) {
        registroDeUso.registrar(modulo, modelo, usuarioId, 0, 0, 0, System.currentTimeMillis() - inicio,
                false, erro != null ? erro : excecao.getClass().getSimpleName());
    }

    private void registrar(ModuloIa modulo, String modelo, Long usuarioId, Usage uso, long duracao,
            boolean sucesso, String erro) {
        registroDeUso.registrar(modulo, modelo, usuarioId, uso.inputTokens(), uso.outputTokens(),
                uso.cacheReadInputTokens().orElse(0L), duracao, sucesso, erro);
    }
}
