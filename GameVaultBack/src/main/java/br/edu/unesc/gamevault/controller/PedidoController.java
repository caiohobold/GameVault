package br.edu.unesc.gamevault.controller;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.unesc.gamevault.dto.request.PedidoRequest;
import br.edu.unesc.gamevault.dto.response.PedidoResponse;
import br.edu.unesc.gamevault.entity.enums.StatusPedido;
import br.edu.unesc.gamevault.security.UsuarioAutenticado;
import br.edu.unesc.gamevault.service.PedidoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Pedidos", description = "Checkout, pagamento com a carteira virtual e histórico. O pedido é sempre do usuário do token.")
@RestController
@RequestMapping("/pedidos")
@RequiredArgsConstructor
public class PedidoController {
    private final PedidoService pedidoService;

    @Operation(summary = "Lista os pedidos do usuário autenticado",
            description = "Pode filtrar por status. O usuário nunca enxerga pedido de outra pessoa.")
    @ApiResponse(responseCode = "200", description = "Página de pedidos")
    @GetMapping("/meus")
    public ResponseEntity<Page<PedidoResponse>> listarMeus(
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @RequestParam(required = false) StatusPedido status,
            @PageableDefault(size = 20, sort = "dataPedido", direction = Sort.Direction.DESC) Pageable paginacao) {
        return ResponseEntity.ok(pedidoService.listarDoUsuario(autenticado.getId(), status, paginacao));
    }

    @Operation(summary = "Detalha um pedido",
            description = "Devolve 403 se o pedido pertencer a outro usuário, salvo para o ADMIN.")
    @ApiResponse(responseCode = "200", description = "Pedido encontrado")
    @ApiResponse(responseCode = "404", description = "Pedido inexistente", content = @Content(schema = @Schema(ref = "#/components/schemas/ErroResposta")))
    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> buscarPorId(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        return ResponseEntity.ok(pedidoService.buscarPorIdDoUsuario(id, autenticado.getId()));
    }

    @Operation(summary = "Cria um pedido",
            description = """
                    O corpo leva apenas a lista de jogos: o dono do pedido vem do token.
                    O servidor calcula o total, aplica a promoção vigente e **congela o preço**
                    de cada item. Jogo que já está na biblioteca é recusado com 422.
                    O pedido nasce `PENDENTE` — é o pagamento que libera a biblioteca.
                    """)
    @ApiResponse(responseCode = "201", description = "Pedido criado com status PENDENTE")
    @ApiResponse(responseCode = "422", description = "Jogo repetido, já possuído ou desativado",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErroResposta")))
    @PostMapping
    public ResponseEntity<PedidoResponse> criar(
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Valid @RequestBody PedidoRequest requisicao) {
        PedidoResponse criado = pedidoService.criar(requisicao, autenticado.getId());
        return ResponseEntity.created(URI.create("/pedidos/" + criado.id())).body(criado);
    }

    @Operation(summary = "Paga o pedido com o saldo da carteira",
            description = """
                    Debita o saldo do usuário e insere os jogos na biblioteca, numa única transação.
                    Saldo insuficiente devolve 422 e nada é alterado.
                    """)
    @ApiResponse(responseCode = "200", description = "Pedido pago e jogos liberados na biblioteca")
    @ApiResponse(responseCode = "422", description = "Saldo insuficiente ou pedido já não está PENDENTE",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErroResposta")))
    @PostMapping("/{id}/pagar")
    public ResponseEntity<PedidoResponse> pagar(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        return ResponseEntity.ok(pedidoService.pagar(id, autenticado.getId()));
    }

    @Operation(summary = "Cancela um pedido pendente",
            description = "Pedido já pago é imutável e devolve 422.")
    @ApiResponse(responseCode = "200", description = "Pedido cancelado")
    @ApiResponse(responseCode = "422", description = "O pedido não está PENDENTE", content = @Content(schema = @Schema(ref = "#/components/schemas/ErroResposta")))
    @PostMapping("/{id}/cancelar")
    public ResponseEntity<PedidoResponse> cancelar(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        return ResponseEntity.ok(pedidoService.cancelar(id, autenticado.getId()));
    }

    @Operation(summary = "Exclui um pedido", description = "Pedido pago não pode ser excluído.")
    @ApiResponse(responseCode = "204", description = "Pedido excluído")
    @ApiResponse(responseCode = "422", description = "Tentativa de excluir pedido pago", content = @Content(schema = @Schema(ref = "#/components/schemas/ErroResposta")))
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        pedidoService.excluir(id, autenticado.getId());
        return ResponseEntity.noContent().build();
    }
}
