package br.edu.unesc.gamevault.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.unesc.gamevault.dto.request.ModeracaoRevisaoRequest;
import br.edu.unesc.gamevault.dto.response.AvaliacaoResponse;
import br.edu.unesc.gamevault.entity.enums.StatusModeracao;
import br.edu.unesc.gamevault.security.UsuarioAutenticado;
import br.edu.unesc.gamevault.service.AvaliacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "IA - Moderação", description = "Fila de avaliações que a moderação automática mandou para revisão humana.")
@RestController
@RequestMapping("/avaliacoes")
@RequiredArgsConstructor
public class ModeracaoController {
    private final AvaliacaoService avaliacaoService;

    @Operation(summary = "Lista avaliações por situação de moderação",
            description = "Por padrão traz as que estão EM_REVISAO, mais antigas primeiro.")
    @ApiResponse(responseCode = "200", description = "Página de avaliações")
    @GetMapping("/moderacao")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AvaliacaoResponse>> listar(
            @Parameter(description = "APROVADA, EM_REVISAO ou BLOQUEADA", example = "EM_REVISAO")
            @RequestParam(required = false) StatusModeracao status,
            @PageableDefault(size = 20, sort = "dataAvaliacao", direction = Sort.Direction.ASC) Pageable paginacao) {
        return ResponseEntity.ok(avaliacaoService.listarParaModeracao(status, paginacao));
    }

    @Operation(summary = "Aprova ou bloqueia uma avaliação",
            description = "A nota média do jogo é recalculada. Aprovar pode disparar a atualização do resumo.")
    @ApiResponse(responseCode = "200", description = "Avaliação revisada")
    @ApiResponse(responseCode = "422", description = "Decisão diferente de APROVADA ou BLOQUEADA")
    @PatchMapping("/{id}/moderacao")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AvaliacaoResponse> revisar(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Valid @RequestBody ModeracaoRevisaoRequest requisicao) {
        return ResponseEntity.ok(avaliacaoService.revisarModeracao(id, requisicao, autenticado.getId()));
    }
}
