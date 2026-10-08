package br.edu.unesc.gamevault.controller;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.unesc.gamevault.dto.request.CategoriaRequest;
import br.edu.unesc.gamevault.dto.response.CategoriaResponse;
import br.edu.unesc.gamevault.service.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Categorias", description = "Classificações do catálogo. Leitura é pública; manutenção é exclusiva do ADMIN.")
@RestController
@RequestMapping("/categorias")
@RequiredArgsConstructor
public class CategoriaController {
    private final CategoriaService categoriaService;

    @Operation(summary = "Lista as categorias", description = "Rota pública, paginada e com busca por nome.")
    @ApiResponse(responseCode = "200", description = "Página de categorias")
    @SecurityRequirements
    @GetMapping
    public ResponseEntity<Page<CategoriaResponse>> listar(
            @RequestParam(required = false) String nome,
            @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) Pageable paginacao) {
        return ResponseEntity.ok(categoriaService.listar(nome, paginacao));
    }

    @Operation(summary = "Busca uma categoria pelo id")
    @ApiResponse(responseCode = "200", description = "Categoria encontrada")
    @ApiResponse(responseCode = "404", description = "Categoria inexistente",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErroResposta")))
    @SecurityRequirements
    @GetMapping("/{id}")
    public ResponseEntity<CategoriaResponse> buscarPorId(
            @Parameter(description = "Identificador da categoria", example = "1") @PathVariable Long id) {
        return ResponseEntity.ok(categoriaService.buscarPorId(id));
    }

    @Operation(summary = "Cria uma categoria", description = "Exclusivo do ADMIN. O nome é único.")
    @ApiResponse(responseCode = "201", description = "Categoria criada")
    @ApiResponse(responseCode = "409", description = "Já existe categoria com este nome",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErroResposta")))
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoriaResponse> criar(@Valid @RequestBody CategoriaRequest requisicao) {
        CategoriaResponse criada = categoriaService.criar(requisicao);
        return ResponseEntity.created(URI.create("/categorias/" + criada.id())).body(criada);
    }

    @Operation(summary = "Atualiza uma categoria", description = "Exclusivo do ADMIN.")
    @ApiResponse(responseCode = "200", description = "Categoria atualizada")
    @ApiResponse(responseCode = "404", description = "Categoria inexistente",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErroResposta")))
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoriaResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody CategoriaRequest requisicao) {
        return ResponseEntity.ok(categoriaService.atualizar(id, requisicao));
    }

    @Operation(summary = "Exclui uma categoria",
            description = "Exclusivo do ADMIN. Falha com 409 se ainda houver jogo classificado nela.")
    @ApiResponse(responseCode = "204", description = "Categoria excluída")
    @ApiResponse(responseCode = "409", description = "Categoria em uso por algum jogo",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErroResposta")))
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        categoriaService.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
