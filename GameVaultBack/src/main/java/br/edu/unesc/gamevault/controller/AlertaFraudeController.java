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

import br.edu.unesc.gamevault.dto.request.RevisaoAlertaRequest;
import br.edu.unesc.gamevault.dto.response.AlertaFraudeResponse;
import br.edu.unesc.gamevault.entity.enums.StatusAlertaFraude;
import br.edu.unesc.gamevault.ia.fraude.DeteccaoFraudeService;
import br.edu.unesc.gamevault.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "IA - Fraude", description = "Alertas da detecção de anomalias. A IA aponta; o administrador decide.")
@RestController
@RequestMapping("/admin/alertas")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AlertaFraudeController {
    private final DeteccaoFraudeService deteccaoService;

    @Operation(summary = "Lista alertas de fraude", description = "Por padrão os ABERTOS, mais suspeitos primeiro.")
    @ApiResponse(responseCode = "200", description = "Página de alertas")
    @GetMapping
    public ResponseEntity<Page<AlertaFraudeResponse>> listar(
            @Parameter(description = "ABERTO, CONFIRMADO ou DESCARTADO", example = "ABERTO")
            @RequestParam(required = false) StatusAlertaFraude status,
            @PageableDefault(size = 20, sort = "pontuacao", direction = Sort.Direction.DESC) Pageable paginacao) {
        return ResponseEntity.ok(deteccaoService.listar(status, paginacao));
    }

    @Operation(summary = "Confirma ou descarta um alerta",
            description = """
                    A decisão vira rótulo para o classificador supervisionado. Para desativar a conta, use
                    PATCH /usuarios/{id}/ativo.
                    """)
    @ApiResponse(responseCode = "200", description = "Alerta revisado")
    @PatchMapping("/{id}")
    public ResponseEntity<AlertaFraudeResponse> revisar(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Valid @RequestBody RevisaoAlertaRequest requisicao) {
        return ResponseEntity.ok(deteccaoService.revisar(id, requisicao, autenticado.getId()));
    }
}
