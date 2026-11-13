package br.edu.unesc.gamevault.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.unesc.gamevault.dto.request.MensagemAssistenteRequest;
import br.edu.unesc.gamevault.dto.response.ConversaAssistenteResponse;
import br.edu.unesc.gamevault.dto.response.RespostaAssistenteResponse;
import br.edu.unesc.gamevault.ia.chat.AssistenteChatService;
import br.edu.unesc.gamevault.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "IA - Chat da loja", description = "Assistente que conversa com o jogador e consulta a loja em nome dele.")
@RestController
@RequestMapping("/assistente")
@RequiredArgsConstructor
public class AssistenteChatController {
    private final AssistenteChatService assistenteService;

    @Operation(summary = "Envia uma mensagem ao assistente",
            description = """
                    Sem `conversaId`, começa uma conversa nova. O assistente consulta catálogo, promoções,
                    pedidos, biblioteca, lista de desejos e saldo do usuário autenticado. Para pagar, ele só
                    devolve uma ação pendente em `acoesPendentes`, que o usuário confirma na tela.
                    Limite de 30 mensagens por hora.
                    """)
    @ApiResponse(responseCode = "200", description = "Resposta do assistente")
    @ApiResponse(responseCode = "429", description = "Limite de mensagens por hora atingido")
    @ApiResponse(responseCode = "503", description = "IA indisponível")
    @PostMapping("/mensagens")
    public ResponseEntity<RespostaAssistenteResponse> enviar(
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Valid @RequestBody MensagemAssistenteRequest requisicao) {
        return ResponseEntity.ok(assistenteService.enviar(requisicao, autenticado.getId()));
    }

    @Operation(summary = "Lista as conversas do usuário autenticado", description = "As 20 mais recentes.")
    @GetMapping("/conversas")
    public ResponseEntity<List<ConversaAssistenteResponse>> listar(
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        return ResponseEntity.ok(assistenteService.listarDoUsuario(autenticado.getId()));
    }

    @Operation(summary = "Mostra uma conversa", description = "Só o dono da conversa tem acesso.")
    @ApiResponse(responseCode = "403", description = "A conversa é de outro usuário")
    @GetMapping("/conversas/{id}")
    public ResponseEntity<ConversaAssistenteResponse> buscar(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        return ResponseEntity.ok(assistenteService.buscar(id, autenticado.getId()));
    }

    @Operation(summary = "Apaga uma conversa", description = "Só o dono da conversa pode apagar.")
    @DeleteMapping("/conversas/{id}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        assistenteService.excluir(id, autenticado.getId());
        return ResponseEntity.noContent().build();
    }
}
