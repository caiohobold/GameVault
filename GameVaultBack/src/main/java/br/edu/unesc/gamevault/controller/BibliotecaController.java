package br.edu.unesc.gamevault.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.unesc.gamevault.dto.request.BibliotecaHorasRequest;
import br.edu.unesc.gamevault.dto.response.BibliotecaResponse;
import br.edu.unesc.gamevault.security.UsuarioAutenticado;
import br.edu.unesc.gamevault.service.BibliotecaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Biblioteca", description = "Jogos que o usuário possui. Os itens entram automaticamente quando um pedido é pago.")
@RestController
@RequestMapping("/biblioteca")
@RequiredArgsConstructor
public class BibliotecaController {
    private final BibliotecaService bibliotecaService;

    @Operation(summary = "Lista a biblioteca do usuário autenticado",
            description = "Traz data de aquisição e horas jogadas de cada título.")
    @ApiResponse(responseCode = "200", description = "Página de itens da biblioteca")
    @GetMapping("/minha")
    public ResponseEntity<Page<BibliotecaResponse>> listarMinha(
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @PageableDefault(size = 20, sort = "dataAquisicao", direction = Sort.Direction.DESC) Pageable paginacao) {
        return ResponseEntity.ok(bibliotecaService.listarDoUsuario(autenticado.getId(), paginacao));
    }

    @Operation(summary = "Detalha um item da biblioteca",
            description = "Devolve 403 se o item pertencer a outro usuário.")
    @ApiResponse(responseCode = "200", description = "Item encontrado")
    @ApiResponse(responseCode = "404", description = "Item inexistente", content = @Content(schema = @Schema(ref = "#/components/schemas/ErroResposta")))
    @GetMapping("/{id}")
    public ResponseEntity<BibliotecaResponse> buscarPorId(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        return ResponseEntity.ok(bibliotecaService.buscarPorIdDoUsuario(id, autenticado.getId()));
    }

    @Operation(summary = "Registra as horas jogadas",
            description = "Só o dono do item atualiza as próprias horas.")
    @ApiResponse(responseCode = "200", description = "Horas atualizadas")
    @PatchMapping("/{id}/horas")
    public ResponseEntity<BibliotecaResponse> atualizarHoras(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Valid @RequestBody BibliotecaHorasRequest requisicao) {
        return ResponseEntity.ok(bibliotecaService.atualizarHoras(id, requisicao, autenticado.getId()));
    }
}
