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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.unesc.gamevault.dto.request.AvaliacaoAtualizacaoRequest;
import br.edu.unesc.gamevault.dto.request.AvaliacaoRequest;
import br.edu.unesc.gamevault.dto.response.AvaliacaoResponse;
import br.edu.unesc.gamevault.security.UsuarioAutenticado;
import br.edu.unesc.gamevault.service.AvaliacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Avaliações", description = "Notas e comentários. Só avalia quem tem o jogo na própria biblioteca.")
@RestController
@RequestMapping("/avaliacoes")
@RequiredArgsConstructor
public class AvaliacaoController {
    private final AvaliacaoService avaliacaoService;

    @Operation(summary = "Lista as avaliações do usuário autenticado",
            description = "Para ver as avaliações de um jogo, use `GET /jogos/{id}/avaliacoes`, que é público.")
    @ApiResponse(responseCode = "200", description = "Página de avaliações próprias")
    @GetMapping
    public ResponseEntity<Page<AvaliacaoResponse>> listarMinhas(
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @PageableDefault(size = 20, sort = "dataAvaliacao", direction = Sort.Direction.DESC) Pageable paginacao) {
        return ResponseEntity.ok(avaliacaoService.listarPorUsuario(autenticado.getId(), paginacao));
    }

    @Operation(summary = "Detalha uma avaliação")
    @ApiResponse(responseCode = "200", description = "Avaliação encontrada")
    @ApiResponse(responseCode = "404", description = "Avaliação inexistente", content = @Content(schema = @Schema(ref = "#/components/schemas/ErroResposta")))
    @GetMapping("/{id}")
    public ResponseEntity<AvaliacaoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(avaliacaoService.buscarPorId(id));
    }

    @Operation(summary = "Avalia um jogo",
            description = """
                    Só é possível avaliar jogo presente na própria biblioteca — caso contrário, 403.
                    Cada usuário avalia um jogo uma única vez, garantido por constraint no banco.
                    A cada avaliação a nota média e o total do jogo são recalculados.
                    """)
    @ApiResponse(responseCode = "201", description = "Avaliação publicada e nota média recalculada")
    @ApiResponse(responseCode = "403", description = "O usuário não possui o jogo", content = @Content(schema = @Schema(ref = "#/components/schemas/ErroResposta")))
    @ApiResponse(responseCode = "409", description = "O usuário já avaliou este jogo", content = @Content(schema = @Schema(ref = "#/components/schemas/ErroResposta")))
    @PostMapping
    public ResponseEntity<AvaliacaoResponse> criar(
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Valid @RequestBody AvaliacaoRequest requisicao) {
        AvaliacaoResponse criada = avaliacaoService.criar(requisicao, autenticado.getId());
        return ResponseEntity.created(URI.create("/avaliacoes/" + criada.id())).body(criada);
    }

    @Operation(summary = "Edita a própria avaliação",
            description = "Recalcula a nota média do jogo. Editar avaliação de outro usuário devolve 403.")
    @ApiResponse(responseCode = "200", description = "Avaliação atualizada")
    @PutMapping("/{id}")
    public ResponseEntity<AvaliacaoResponse> atualizar(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Valid @RequestBody AvaliacaoAtualizacaoRequest requisicao) {
        return ResponseEntity.ok(avaliacaoService.atualizar(id, requisicao, autenticado.getId()));
    }

    @Operation(summary = "Exclui a própria avaliação",
            description = "O ADMIN pode remover qualquer avaliação. A nota média do jogo é recalculada.")
    @ApiResponse(responseCode = "204", description = "Avaliação excluída")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        avaliacaoService.excluir(id, autenticado.getId());
        return ResponseEntity.noContent().build();
    }
}
