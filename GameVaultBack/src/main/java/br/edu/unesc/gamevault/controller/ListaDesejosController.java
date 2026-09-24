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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.unesc.gamevault.dto.request.ListaDesejosRequest;
import br.edu.unesc.gamevault.dto.response.ListaDesejosResponse;
import br.edu.unesc.gamevault.service.ListaDesejosService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/lista-desejos")
@RequiredArgsConstructor
public class ListaDesejosController {
    private final ListaDesejosService listaDesejosService;

    @GetMapping
    public ResponseEntity<Page<ListaDesejosResponse>> listar(
            @RequestParam Long usuarioId,
            @PageableDefault(size = 20, sort = "dataAdicao", direction = Sort.Direction.DESC) Pageable paginacao) {
        return ResponseEntity.ok(listaDesejosService.listarDoUsuario(usuarioId, paginacao));
    }

    @PostMapping
    public ResponseEntity<ListaDesejosResponse> adicionar(
            @Valid @RequestBody ListaDesejosRequest requisicao) {
        ListaDesejosResponse criado = listaDesejosService.adicionar(requisicao);
        return ResponseEntity.created(URI.create("/lista-desejos/" + criado.id())).body(criado);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        listaDesejosService.remover(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> removerPorUsuarioEJogo(
            @RequestParam Long usuarioId,
            @RequestParam Long jogoId) {
        listaDesejosService.removerPorUsuarioEJogo(usuarioId, jogoId);
        return ResponseEntity.noContent().build();
    }
}
