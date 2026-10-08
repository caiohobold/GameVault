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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.unesc.gamevault.dto.request.PromocaoRequest;
import br.edu.unesc.gamevault.dto.response.PromocaoResponse;
import br.edu.unesc.gamevault.security.UsuarioAutenticado;
import br.edu.unesc.gamevault.service.PromocaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Promoções", description = "Descontos por período. Consultar é público; criar exige ser a publicadora dona do jogo.")
@RestController
@RequestMapping("/promocoes")
@RequiredArgsConstructor
public class PromocaoController {
    private final PromocaoService promocaoService;

    @Operation(summary = "Lista as promoções",
            description = """
                    Rota pública: a vitrine precisa mostrar desconto para quem ainda não entrou.
                    O campo `vigente` indica se a promoção vale na data de hoje.
                    """)
    @ApiResponse(responseCode = "200", description = "Página de promoções")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<Page<PromocaoResponse>> listar(
            @RequestParam(required = false) Long jogoId,
            @PageableDefault(size = 20, sort = "dataInicio", direction = Sort.Direction.DESC) Pageable paginacao) {
        return ResponseEntity.ok(jogoId == null
                ? promocaoService.listarTodas(paginacao)
                : promocaoService.listarPorJogo(jogoId, paginacao));
    }

    @Operation(summary = "Detalha uma promoção")
    @ApiResponse(responseCode = "200", description = "Promoção encontrada")
    @ApiResponse(responseCode = "404", description = "Promoção inexistente", content = @Content(schema = @Schema(ref = "#/components/schemas/ErroResposta")))
    @SecurityRequirements
    @GetMapping("/{id}")
    public ResponseEntity<PromocaoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(promocaoService.buscarPorId(id));
    }

    @Operation(summary = "Cria uma promoção",
            description = """
                    Só a publicadora dona do jogo cria promoção para ele. O desconto fica entre
                    1% e 90%, e `dataFim` precisa ser posterior a `dataInicio`.
                    O preço final é sempre calculado no servidor, no momento do pedido.
                    """)
    @ApiResponse(responseCode = "201", description = "Promoção criada")
    @ApiResponse(responseCode = "400", description = "Período ou percentual inválidos", content = @Content(schema = @Schema(ref = "#/components/schemas/ErroResposta")))
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PUBLICADORA')")
    public ResponseEntity<PromocaoResponse> criar(
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Valid @RequestBody PromocaoRequest requisicao) {
        PromocaoResponse criada = promocaoService.criar(requisicao, autenticado.getId());
        return ResponseEntity.created(URI.create("/promocoes/" + criada.id())).body(criada);
    }

    @Operation(summary = "Atualiza uma promoção",
            description = "Permite encerrar antes do prazo, alterando o campo `ativa`.")
    @ApiResponse(responseCode = "200", description = "Promoção atualizada")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PUBLICADORA')")
    public ResponseEntity<PromocaoResponse> atualizar(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Valid @RequestBody PromocaoRequest requisicao) {
        return ResponseEntity.ok(promocaoService.atualizar(id, requisicao, autenticado.getId()));
    }

    @Operation(summary = "Exclui uma promoção")
    @ApiResponse(responseCode = "204", description = "Promoção excluída")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PUBLICADORA')")
    public ResponseEntity<Void> excluir(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        promocaoService.excluir(id, autenticado.getId());
        return ResponseEntity.noContent().build();
    }
}
