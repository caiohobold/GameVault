package br.edu.unesc.gamevault.controller;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
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
import br.edu.unesc.gamevault.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {
    private final UsuarioService usuarioService;

    @GetMapping
    public ResponseEntity<Page<UsuarioResponse>> listar(
            @RequestParam(required = false) String nome,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Boolean apenasAtivos,
            @PageableDefault(size = 20, sort = "nome", direction = Sort.Direction.ASC) Pageable paginacao) {
        return ResponseEntity.ok(usuarioService.listar(nome, role, apenasAtivos, paginacao));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody UsuarioRequest requisicao) {
        UsuarioResponse criado = usuarioService.criar(requisicao);
        return ResponseEntity.created(URI.create("/usuarios/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioAtualizacaoRequest requisicao) {
        return ResponseEntity.ok(usuarioService.atualizar(id, requisicao));
    }

    @PatchMapping("/{id}/ativo")
    public ResponseEntity<UsuarioResponse> alterarAtivo(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioAtivoRequest requisicao) {
        return ResponseEntity.ok(usuarioService.alterarAtivo(id, requisicao));
    }
}
