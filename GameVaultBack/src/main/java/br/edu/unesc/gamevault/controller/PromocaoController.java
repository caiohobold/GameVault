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

import br.edu.unesc.gamevault.dto.request.PromocaoRequest;
import br.edu.unesc.gamevault.dto.response.PromocaoResponse;
import br.edu.unesc.gamevault.service.PromocaoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/promocoes")
@RequiredArgsConstructor
public class PromocaoController {
    private final PromocaoService promocaoService;

    @GetMapping
    public ResponseEntity<Page<PromocaoResponse>> listar(
            @RequestParam(required = false) Long jogoId,
            @PageableDefault(size = 20, sort = "dataInicio", direction = Sort.Direction.DESC) Pageable paginacao) {
        return ResponseEntity.ok(jogoId == null
                ? promocaoService.listarTodas(paginacao)
                : promocaoService.listarPorJogo(jogoId, paginacao));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PromocaoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(promocaoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<PromocaoResponse> criar(
            @RequestParam(required = false) Long solicitanteId,
            @Valid @RequestBody PromocaoRequest requisicao) {
        PromocaoResponse criada = promocaoService.criar(requisicao, solicitanteId);
        return ResponseEntity.created(URI.create("/promocoes/" + criada.id())).body(criada);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PromocaoResponse> atualizar(
            @PathVariable Long id,
            @RequestParam(required = false) Long solicitanteId,
            @Valid @RequestBody PromocaoRequest requisicao) {
        return ResponseEntity.ok(promocaoService.atualizar(id, requisicao, solicitanteId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long id,
            @RequestParam(required = false) Long solicitanteId) {
        promocaoService.excluir(id, solicitanteId);
        return ResponseEntity.noContent().build();
    }
}
