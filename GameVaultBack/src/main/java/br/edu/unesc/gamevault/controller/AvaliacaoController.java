package br.edu.unesc.gamevault.controller;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.unesc.gamevault.dto.request.AvaliacaoAtualizacaoRequest;
import br.edu.unesc.gamevault.dto.request.AvaliacaoRequest;
import br.edu.unesc.gamevault.dto.response.AvaliacaoResponse;
import br.edu.unesc.gamevault.service.AvaliacaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/avaliacoes")
@RequiredArgsConstructor
public class AvaliacaoController {
    private final AvaliacaoService avaliacaoService;

    @GetMapping
    public ResponseEntity<Page<AvaliacaoResponse>> listarPorUsuario(
            @RequestParam Long usuarioId,
            @PageableDefault(size = 20, sort = "dataAvaliacao", direction = Sort.Direction.DESC) Pageable paginacao) {
        return ResponseEntity.ok(avaliacaoService.listarPorUsuario(usuarioId, paginacao));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AvaliacaoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(avaliacaoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<AvaliacaoResponse> criar(@Valid @RequestBody AvaliacaoRequest requisicao) {
        AvaliacaoResponse criada = avaliacaoService.criar(requisicao);
        return ResponseEntity.created(URI.create("/avaliacoes/" + criada.id())).body(criada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<AvaliacaoResponse> atualizar(
            @PathVariable Long id,
            @RequestParam(required = false) Long solicitanteId,
            @Valid @RequestBody AvaliacaoAtualizacaoRequest requisicao) {
        return ResponseEntity.ok(avaliacaoService.atualizar(id, requisicao, solicitanteId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long id,
            @RequestParam(required = false) Long solicitanteId) {
        avaliacaoService.excluir(id, solicitanteId);
        return ResponseEntity.noContent().build();
    }
}
