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

import br.edu.unesc.gamevault.dto.request.ListaDesejosRequest;
import br.edu.unesc.gamevault.dto.response.ListaDesejosResponse;
import br.edu.unesc.gamevault.security.UsuarioAutenticado;
import br.edu.unesc.gamevault.service.ListaDesejosService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Lista de desejos", description = "Jogos que o usuário pretende comprar depois.")
@RestController
@RequestMapping("/lista-desejos")
@RequiredArgsConstructor
public class ListaDesejosController {
    private final ListaDesejosService listaDesejosService;

    @Operation(summary = "Lista a lista de desejos do usuário autenticado")
    @ApiResponse(responseCode = "200", description = "Página de itens desejados")
    @GetMapping
    public ResponseEntity<Page<ListaDesejosResponse>> listar(
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @PageableDefault(size = 20, sort = "dataAdicao", direction = Sort.Direction.DESC) Pageable paginacao) {
        return ResponseEntity.ok(listaDesejosService.listarDoUsuario(autenticado.getId(), paginacao));
    }

    @Operation(summary = "Adiciona um jogo à lista de desejos",
            description = "Jogo que já está na biblioteca é recusado, e o mesmo jogo não entra duas vezes.")
    @ApiResponse(responseCode = "201", description = "Jogo adicionado")
    @ApiResponse(responseCode = "409", description = "O jogo já está na lista", content = @Content(schema = @Schema(ref = "#/components/schemas/ErroResposta")))
    @ApiResponse(responseCode = "422", description = "O jogo já está na biblioteca", content = @Content(schema = @Schema(ref = "#/components/schemas/ErroResposta")))
    @PostMapping
    public ResponseEntity<ListaDesejosResponse> adicionar(
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Valid @RequestBody ListaDesejosRequest requisicao) {
        ListaDesejosResponse criado = listaDesejosService.adicionar(requisicao, autenticado.getId());
        return ResponseEntity.created(URI.create("/lista-desejos/" + criado.id())).body(criado);
    }

    @Operation(summary = "Remove um item da lista pelo id do item",
            description = "Remover item de outro usuário devolve 403.")
    @ApiResponse(responseCode = "204", description = "Item removido")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        listaDesejosService.remover(id, autenticado.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Remove um jogo da lista pelo id do jogo",
            description = "Atalho para o botão de desfazer na tela do jogo.")
    @ApiResponse(responseCode = "204", description = "Jogo removido da lista")
    @DeleteMapping
    public ResponseEntity<Void> removerPorJogo(
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @RequestParam Long jogoId) {
        listaDesejosService.removerPorUsuarioEJogo(autenticado.getId(), jogoId);
        return ResponseEntity.noContent().build();
    }
}
