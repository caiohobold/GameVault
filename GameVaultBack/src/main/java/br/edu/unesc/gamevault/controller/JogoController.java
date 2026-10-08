package br.edu.unesc.gamevault.controller;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.unesc.gamevault.dto.request.JogoRequest;
import br.edu.unesc.gamevault.dto.response.AvaliacaoResponse;
import br.edu.unesc.gamevault.dto.response.JogoResponse;
import br.edu.unesc.gamevault.dto.response.VendaResponse;
import br.edu.unesc.gamevault.security.UsuarioAutenticado;
import br.edu.unesc.gamevault.service.AvaliacaoService;
import br.edu.unesc.gamevault.service.JogoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Jogos", description = "Catálogo da loja. A vitrine é pública; publicar e editar exige ser a publicadora dona do jogo.")
@RestController
@RequestMapping("/jogos")
@RequiredArgsConstructor
public class JogoController {
    private final JogoService jogoService;
    private final AvaliacaoService avaliacaoService;

    @Operation(summary = "Lista o catálogo",
            description = """
                    Rota pública. Aceita busca parcial por título e filtro por categoria, além da
                    paginação padrão (`page`, `size`, `sort`). Jogos desativados não aparecem aqui.
                    """)
    @ApiResponse(responseCode = "200", description = "Página de jogos ativos")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<Page<JogoResponse>> listar(
            @Parameter(description = "Busca parcial por título", example = "sombras")
            @RequestParam(required = false) String titulo,
            @Parameter(description = "Filtra por categoria", example = "3")
            @RequestParam(required = false) Long categoriaId,
            @PageableDefault(size = 20, sort = "titulo", direction = Sort.Direction.ASC) Pageable paginacao) {
        return ResponseEntity.ok(jogoService.listar(titulo, categoriaId, paginacao));
    }

    @Operation(summary = "Lista os jogos da publicadora autenticada",
            description = "Inclui os jogos desativados, que não aparecem no catálogo público.")
    @ApiResponse(responseCode = "200", description = "Página de jogos da própria publicadora")
    @GetMapping("/meus")
    @PreAuthorize("hasAnyRole('ADMIN', 'PUBLICADORA')")
    public ResponseEntity<Page<JogoResponse>> listarDaPublicadora(
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @PageableDefault(size = 20, sort = "titulo", direction = Sort.Direction.ASC) Pageable paginacao) {
        return ResponseEntity.ok(jogoService.listarDaPublicadora(autenticado.getId(), paginacao));
    }

    @Operation(summary = "Resumo de vendas dos jogos da publicadora",
            description = "Quantidade vendida e valor arrecadado por jogo, contando apenas pedidos pagos.")
    @ApiResponse(responseCode = "200", description = "Página com o resumo por jogo")
    @GetMapping("/meus/vendas")
    @PreAuthorize("hasAnyRole('ADMIN', 'PUBLICADORA')")
    public ResponseEntity<Page<VendaResponse>> resumirVendas(
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @PageableDefault(size = 20) Pageable paginacao) {
        return ResponseEntity.ok(jogoService.resumirVendas(autenticado.getId(), paginacao));
    }

    @Operation(summary = "Detalha um jogo", description = "Rota pública.")
    @ApiResponse(responseCode = "200", description = "Jogo encontrado")
    @ApiResponse(responseCode = "404", description = "Jogo inexistente", content = @Content(schema = @Schema(ref = "#/components/schemas/ErroResposta")))
    @SecurityRequirements
    @GetMapping("/{id}")
    public ResponseEntity<JogoResponse> buscarPorId(
            @Parameter(description = "Identificador do jogo", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(jogoService.buscarPorId(id));
    }

    @Operation(summary = "Lista as avaliações de um jogo", description = "Rota pública e paginada.")
    @ApiResponse(responseCode = "200", description = "Página de avaliações")
    @SecurityRequirements
    @GetMapping("/{id}/avaliacoes")
    public ResponseEntity<Page<AvaliacaoResponse>> listarAvaliacoes(
            @PathVariable Long id,
            @PageableDefault(size = 20, sort = "dataAvaliacao", direction = Sort.Direction.DESC) Pageable paginacao) {
        return ResponseEntity.ok(avaliacaoService.listarPorJogo(id, paginacao));
    }

    @Operation(summary = "Publica um jogo no catálogo",
            description = """
                    O campo `publicadoraId` do corpo **só é lido quando quem chama é ADMIN**.
                    Uma publicadora sempre registra o jogo em nome próprio, o que impede publicar
                    no nome de outra.
                    """)
    @ApiResponse(responseCode = "201", description = "Jogo publicado")
    @ApiResponse(responseCode = "422", description = "ADMIN não informou a publicadora, ou o papel não permite publicar",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErroResposta")))
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PUBLICADORA')")
    public ResponseEntity<JogoResponse> criar(
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Valid @RequestBody JogoRequest requisicao) {
        JogoResponse criado = jogoService.criar(requisicao, autenticado.getId());
        return ResponseEntity.created(URI.create("/jogos/" + criado.id())).body(criado);
    }

    @Operation(summary = "Atualiza um jogo",
            description = "Uma publicadora só edita os próprios jogos; tentar editar o de outra devolve 403.")
    @ApiResponse(responseCode = "200", description = "Jogo atualizado")
    @ApiResponse(responseCode = "404", description = "Jogo inexistente", content = @Content(schema = @Schema(ref = "#/components/schemas/ErroResposta")))
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PUBLICADORA')")
    public ResponseEntity<JogoResponse> atualizar(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Valid @RequestBody JogoRequest requisicao) {
        return ResponseEntity.ok(jogoService.atualizar(id, requisicao, autenticado.getId()));
    }

    @Operation(summary = "Desativa um jogo",
            description = """
                    É *soft delete*: o registro continua no banco com `ativo = false`, porque
                    pedidos e bibliotecas antigos precisam da referência. O jogo some do catálogo
                    e não pode mais ser comprado.
                    """)
    @ApiResponse(responseCode = "204", description = "Jogo desativado")
    @ApiResponse(responseCode = "404", description = "Jogo inexistente", content = @Content(schema = @Schema(ref = "#/components/schemas/ErroResposta")))
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PUBLICADORA')")
    public ResponseEntity<Void> desativar(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        jogoService.desativar(id, autenticado.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Reativa um jogo desativado",
            description = "Devolve o jogo ao catálogo público.")
    @ApiResponse(responseCode = "200", description = "Jogo reativado")
    @PatchMapping("/{id}/reativar")
    @PreAuthorize("hasAnyRole('ADMIN', 'PUBLICADORA')")
    public ResponseEntity<JogoResponse> reativar(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        return ResponseEntity.ok(jogoService.reativar(id, autenticado.getId()));
    }
}
