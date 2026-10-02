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
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/jogos")
@RequiredArgsConstructor
public class JogoController {
    private final JogoService jogoService;
    private final AvaliacaoService avaliacaoService;

    @GetMapping
    public ResponseEntity<Page<JogoResponse>> listar(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) Long categoriaId,
            @PageableDefault(size = 20, sort = "titulo", direction = Sort.Direction.ASC) Pageable paginacao) {
        return ResponseEntity.ok(jogoService.listar(titulo, categoriaId, paginacao));
    }

    @GetMapping("/meus")
    @PreAuthorize("hasAnyRole('ADMIN', 'PUBLICADORA')")
    public ResponseEntity<Page<JogoResponse>> listarDaPublicadora(
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @PageableDefault(size = 20, sort = "titulo", direction = Sort.Direction.ASC) Pageable paginacao) {
        return ResponseEntity.ok(jogoService.listarDaPublicadora(autenticado.getId(), paginacao));
    }

    @GetMapping("/meus/vendas")
    @PreAuthorize("hasAnyRole('ADMIN', 'PUBLICADORA')")
    public ResponseEntity<Page<VendaResponse>> resumirVendas(
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @PageableDefault(size = 20) Pageable paginacao) {
        return ResponseEntity.ok(jogoService.resumirVendas(autenticado.getId(), paginacao));
    }

    @GetMapping("/{id}")
    public ResponseEntity<JogoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(jogoService.buscarPorId(id));
    }

    @GetMapping("/{id}/avaliacoes")
    public ResponseEntity<Page<AvaliacaoResponse>> listarAvaliacoes(
            @PathVariable Long id,
            @PageableDefault(size = 20, sort = "dataAvaliacao", direction = Sort.Direction.DESC) Pageable paginacao) {
        return ResponseEntity.ok(avaliacaoService.listarPorJogo(id, paginacao));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PUBLICADORA')")
    public ResponseEntity<JogoResponse> criar(
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Valid @RequestBody JogoRequest requisicao) {
        JogoResponse criado = jogoService.criar(requisicao, autenticado.getId());
        return ResponseEntity.created(URI.create("/jogos/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PUBLICADORA')")
    public ResponseEntity<JogoResponse> atualizar(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Valid @RequestBody JogoRequest requisicao) {
        return ResponseEntity.ok(jogoService.atualizar(id, requisicao, autenticado.getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PUBLICADORA')")
    public ResponseEntity<Void> desativar(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        jogoService.desativar(id, autenticado.getId());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/reativar")
    @PreAuthorize("hasAnyRole('ADMIN', 'PUBLICADORA')")
    public ResponseEntity<JogoResponse> reativar(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        return ResponseEntity.ok(jogoService.reativar(id, autenticado.getId()));
    }
}
