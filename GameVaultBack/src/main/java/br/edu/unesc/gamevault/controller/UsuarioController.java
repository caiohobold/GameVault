package br.edu.unesc.gamevault.controller;

import java.net.URI;

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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.unesc.gamevault.dto.request.UsuarioAtivoRequest;
import br.edu.unesc.gamevault.dto.request.UsuarioAtualizacaoRequest;
import br.edu.unesc.gamevault.dto.request.UsuarioRequest;
import br.edu.unesc.gamevault.dto.response.UsuarioResponse;
import br.edu.unesc.gamevault.entity.enums.Role;
import br.edu.unesc.gamevault.security.UsuarioAutenticado;
import br.edu.unesc.gamevault.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Usuários", description = "Gestão de contas. Listar e desativar é do ADMIN; ver e editar o próprio cadastro é do dono.")
@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioService usuarioService;

    @Operation(summary = "Lista os usuários da plataforma",
            description = "Exclusivo do ADMIN. Permite filtrar por nome, por papel e por contas ativas.")
    @ApiResponse(responseCode = "200", description = "Página de usuários, sem o campo senha")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UsuarioResponse>> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean apenasAtivos,
            @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) Pageable paginacao) {
        return ResponseEntity.ok(usuarioService.listar(nome, role, apenasAtivos, paginacao));
    }

    @Operation(summary = "Busca um usuário pelo id",
            description = "O ADMIN consulta qualquer usuário; os demais, apenas o próprio cadastro.")
    @ApiResponse(responseCode = "200", description = "Usuário encontrado")
    @ApiResponse(responseCode = "404", description = "Usuário inexistente",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErroResposta")))
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id")
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @Operation(summary = "Cria um usuário com papel definido",
            description = """
                    Exclusivo do ADMIN e o único caminho para criar outra conta `ADMIN`.
                    Para auto-cadastro público, use `POST /auth/registrar`.
                    """)
    @ApiResponse(responseCode = "201", description = "Usuário criado")
    @ApiResponse(responseCode = "409", description = "E-mail já cadastrado",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErroResposta")))
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody UsuarioRequest requisicao) {
        UsuarioResponse criado = usuarioService.criar(requisicao);
        return ResponseEntity.created(URI.create("/usuarios/" + criado.id())).body(criado);
    }

    @Operation(summary = "Atualiza um usuário",
            description = """
                    O usuário pode editar o próprio cadastro, mas **só o ADMIN altera o papel**:
                    sem essa checagem, qualquer conta se promoveria a administradora.
                    """)
    @ApiResponse(responseCode = "200", description = "Usuário atualizado")
    @ApiResponse(responseCode = "409", description = "E-mail já usado por outro usuário",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErroResposta")))
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id")
    public ResponseEntity<UsuarioResponse> atualizar(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Valid @RequestBody UsuarioAtualizacaoRequest requisicao) {
        return ResponseEntity.ok(usuarioService.atualizar(id, requisicao, autenticado.getId()));
    }

    @Operation(summary = "Ativa ou desativa uma conta",
            description = "Exclusivo do ADMIN. Conta desativada não consegue autenticar.")
    @ApiResponse(responseCode = "200", description = "Situação da conta alterada")
    @PatchMapping("/{id}/ativo")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> alterarAtivo(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioAtivoRequest requisicao) {
        return ResponseEntity.ok(usuarioService.alterarAtivo(id, requisicao));
    }
}
