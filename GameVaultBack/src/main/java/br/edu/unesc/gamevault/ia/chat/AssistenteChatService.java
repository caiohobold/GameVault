package br.edu.unesc.gamevault.ia.chat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.anthropic.core.ObjectMappers;
import com.anthropic.models.messages.ContentBlock;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageContentParam;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.StopReason;
import com.anthropic.models.messages.TextBlockParam;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUseBlock;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;

import br.edu.unesc.gamevault.dto.request.MensagemAssistenteRequest;
import br.edu.unesc.gamevault.dto.response.AcaoPendenteResponse;
import br.edu.unesc.gamevault.dto.response.ConversaAssistenteResponse;
import br.edu.unesc.gamevault.dto.response.MensagemConversaResponse;
import br.edu.unesc.gamevault.dto.response.RespostaAssistenteResponse;
import br.edu.unesc.gamevault.entity.ConversaAssistente;
import br.edu.unesc.gamevault.entity.MensagemAssistente;
import br.edu.unesc.gamevault.entity.enums.PapelMensagem;
import br.edu.unesc.gamevault.exception.AcessoNegadoException;
import br.edu.unesc.gamevault.exception.IaIndisponivelException;
import br.edu.unesc.gamevault.exception.RecursoNaoEncontradoException;
import br.edu.unesc.gamevault.exception.RegraDeNegocioException;
import br.edu.unesc.gamevault.ia.LimiteDeUsoService;
import br.edu.unesc.gamevault.ia.claude.ClienteClaude;
import br.edu.unesc.gamevault.ia.claude.ModuloIa;
import br.edu.unesc.gamevault.repository.ConversaAssistenteRepository;
import br.edu.unesc.gamevault.repository.MensagemAssistenteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Chat da loja (módulo 8): um agente em que o Claude decide quais ferramentas usar. O
 * laço é nosso, e não o tool runner do SDK, porque cada ferramenta precisa do id do
 * usuário do JWT, que nunca vem do modelo.
 *
 * <p>O histórico só recebe acréscimos: as respostas do Claude são guardadas como vieram
 * (incluindo blocos de raciocínio e de ferramenta) e reenviadas sem alteração. As
 * mensagens novas de um turno só são gravadas quando o turno termina bem; se a IA
 * falhar no meio, a conversa fica como estava.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssistenteChatService {
    private static final int MAXIMO_RODADAS_DE_FERRAMENTAS = 8;
    private static final int MAXIMO_MENSAGENS_POR_CONVERSA = 40;
    private static final JsonMapper JSON_DO_SDK = ObjectMappers.jsonMapper();

    private final ClienteClaude clienteClaude;
    private final FerramentasDoAssistente ferramentas;
    private final LimiteDeUsoService limiteDeUso;
    private final ConversaAssistenteRepository conversaRepository;
    private final MensagemAssistenteRepository mensagemRepository;
    private final TransactionTemplate transacao;

    private record MensagemNova(PapelMensagem papel, MessageParam conteudo) {
    }

    public RespostaAssistenteResponse enviar(MensagemAssistenteRequest requisicao, Long usuarioId) {
        limiteDeUso.registrarChamada(usuarioId, LimiteDeUsoService.CHAT);

        Optional<ConversaAssistente> conversa = Optional.ofNullable(requisicao.conversaId())
                .map(id -> buscarDoUsuario(id, usuarioId));

        List<MessageParam> historico = new ArrayList<>();
        if (conversa.isPresent()) {
            List<MensagemAssistente> anteriores = mensagemRepository.findByConversaIdOrderByIdAsc(conversa.get().getId());
            if (anteriores.size() >= MAXIMO_MENSAGENS_POR_CONVERSA) {
                throw new RegraDeNegocioException(
                        "Esta conversa ficou longa demais. Comece uma nova conversa com o assistente.");
            }
            anteriores.forEach(mensagem -> historico.add(desserializar(mensagem.getConteudo())));
        }

        List<MensagemNova> novas = new ArrayList<>();
        novas.add(new MensagemNova(PapelMensagem.USUARIO, MessageParam.ofUser(requisicao.mensagem().strip())));

        List<AcaoPendenteResponse> acoes = new ArrayList<>();

        for (int rodada = 0; rodada < MAXIMO_RODADAS_DE_FERRAMENTAS; rodada++) {
            List<MessageParam> conversaCompleta = new ArrayList<>(historico);
            novas.forEach(nova -> conversaCompleta.add(nova.conteudo()));

            Message resposta = clienteClaude.conversar(ModuloIa.ASSISTENTE_CHAT, conversaCompleta,
                    ferramentas.definicoes(), usuarioId);
            novas.add(new MensagemNova(PapelMensagem.ASSISTENTE, resposta.toParam()));

            if (StopReason.TOOL_USE.equals(resposta.stopReason().orElse(null))) {
                novas.add(new MensagemNova(PapelMensagem.USUARIO,
                        MessageParam.ofUserOfContentBlockParams(executarFerramentas(resposta, usuarioId, acoes))));
                continue;
            }

            Long conversaId = gravar(conversa, novas, usuarioId);
            String texto = textoDe(resposta);
            log.info("Assistente respondeu ao usuario {} na conversa {} ({} rodadas de ferramentas)", usuarioId,
                    conversaId, rodada);
            return new RespostaAssistenteResponse(conversaId, texto, acoes);
        }

        log.warn("Assistente passou do limite de {} rodadas de ferramentas para o usuario {}",
                MAXIMO_RODADAS_DE_FERRAMENTAS, usuarioId);
        throw new IaIndisponivelException("O assistente não conseguiu concluir a resposta. Tente perguntar de outro jeito.");
    }

    @Transactional(readOnly = true)
    public List<ConversaAssistenteResponse> listarDoUsuario(Long usuarioId) {
        return conversaRepository.findTop20ByUsuarioIdOrderByAtualizadaEmDesc(usuarioId).stream()
                .map(conversa -> new ConversaAssistenteResponse(conversa.getId(), conversa.getCriadaEm(),
                        conversa.getAtualizadaEm(), List.of()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversaAssistenteResponse buscar(Long conversaId, Long usuarioId) {
        ConversaAssistente conversa = buscarDoUsuario(conversaId, usuarioId);

        List<MensagemConversaResponse> mensagens = mensagemRepository.findByConversaIdOrderByIdAsc(conversaId).stream()
                .map(mensagem -> new MensagemConversaResponse(mensagem.getPapel(),
                        textoVisivel(desserializar(mensagem.getConteudo())), mensagem.getCriadaEm()))
                .filter(mensagem -> !mensagem.texto().isBlank())
                .toList();

        return new ConversaAssistenteResponse(conversa.getId(), conversa.getCriadaEm(), conversa.getAtualizadaEm(),
                mensagens);
    }

    @Transactional
    public void excluir(Long conversaId, Long usuarioId) {
        conversaRepository.delete(buscarDoUsuario(conversaId, usuarioId));
        log.info("Conversa {} do assistente excluida pelo usuario {}", conversaId, usuarioId);
    }

    private List<ContentBlockParam> executarFerramentas(Message resposta, Long usuarioId,
            List<AcaoPendenteResponse> acoes) {
        List<ContentBlockParam> resultados = new ArrayList<>();

        // Todos os resultados voltam juntos numa única mensagem, como a API espera
        for (ContentBlock bloco : resposta.content()) {
            Optional<ToolUseBlock> chamada = bloco.toolUse();
            if (chamada.isEmpty()) {
                continue;
            }

            ToolUseBlock ferramenta = chamada.get();
            FerramentasDoAssistente.Resultado resultado = ferramentas.executar(ferramenta.name(), ferramenta._input(),
                    usuarioId, acoes);
            log.debug("Ferramenta {} executada para o usuario {} (erro={})", ferramenta.name(), usuarioId,
                    resultado.erro());

            resultados.add(ContentBlockParam.ofToolResult(ToolResultBlockParam.builder()
                    .toolUseId(ferramenta.id())
                    .content(resultado.conteudo())
                    .isError(resultado.erro())
                    .build()));
        }

        return resultados;
    }

    private Long gravar(Optional<ConversaAssistente> existente, List<MensagemNova> novas, Long usuarioId) {
        return transacao.execute(status -> {
            LocalDateTime agora = LocalDateTime.now();
            ConversaAssistente conversa = existente.orElseGet(() -> ConversaAssistente.builder()
                    .usuarioId(usuarioId)
                    .criadaEm(agora)
                    .build());
            conversa.setAtualizadaEm(agora);
            conversa = conversaRepository.save(conversa);

            for (MensagemNova nova : novas) {
                mensagemRepository.save(MensagemAssistente.builder()
                        .conversaId(conversa.getId())
                        .papel(nova.papel())
                        .conteudo(serializar(nova.conteudo()))
                        .criadaEm(agora)
                        .build());
            }

            return conversa.getId();
        });
    }

    private ConversaAssistente buscarDoUsuario(Long conversaId, Long usuarioId) {
        ConversaAssistente conversa = conversaRepository.findById(conversaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Conversa", conversaId));

        if (!conversa.getUsuarioId().equals(usuarioId)) {
            throw new AcessoNegadoException("Esta conversa pertence a outro usuário");
        }
        return conversa;
    }

    private static String textoDe(Message resposta) {
        return resposta.content().stream()
                .flatMap(bloco -> bloco.text().stream())
                .map(texto -> texto.text())
                .collect(Collectors.joining("\n"))
                .strip();
    }

    /** Só o texto que faz sentido mostrar na tela: sem raciocínio, chamadas ou resultados de ferramenta. */
    static String textoVisivel(MessageParam mensagem) {
        MessageContentParam conteudo = mensagem.isUser() ? mensagem.asUser().content()
                : mensagem.isAssistant() ? mensagem.asAssistant().content() : null;

        if (conteudo == null) {
            return "";
        }
        if (conteudo.isString()) {
            return conteudo.asString();
        }

        return conteudo.asContentBlockParams().stream()
                .flatMap(bloco -> bloco.text().stream())
                .map(TextBlockParam::text)
                .collect(Collectors.joining("\n"))
                .strip();
    }

    static String serializar(MessageParam mensagem) {
        try {
            return JSON_DO_SDK.writeValueAsString(mensagem);
        } catch (JsonProcessingException excecao) {
            throw new IllegalStateException("Não foi possível serializar a mensagem do assistente", excecao);
        }
    }

    static MessageParam desserializar(String json) {
        try {
            return JSON_DO_SDK.readValue(json, MessageParam.class);
        } catch (JsonProcessingException excecao) {
            throw new IllegalStateException("Mensagem do assistente gravada em formato inválido", excecao);
        }
    }
}
