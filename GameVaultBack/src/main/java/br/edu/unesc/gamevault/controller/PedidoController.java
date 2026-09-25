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

import br.edu.unesc.gamevault.dto.request.PedidoRequest;
import br.edu.unesc.gamevault.dto.response.PedidoResponse;
import br.edu.unesc.gamevault.entity.enums.StatusPedido;
import br.edu.unesc.gamevault.service.PedidoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/pedidos")
@RequiredArgsConstructor
public class PedidoController {
    private final PedidoService pedidoService;

    @GetMapping("/meus")
    public ResponseEntity<Page<PedidoResponse>> listarMeus(
            @RequestParam Long usuarioId,
            @RequestParam(required = false) StatusPedido status,
            @PageableDefault(size = 20, sort = "dataPedido", direction = Sort.Direction.DESC) Pageable paginacao) {
        return ResponseEntity.ok(pedidoService.listarDoUsuario(usuarioId, status, paginacao));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(pedidoService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<PedidoResponse> criar(@Valid @RequestBody PedidoRequest requisicao) {
        PedidoResponse criado = pedidoService.criar(requisicao);
        return ResponseEntity.created(URI.create("/pedidos/" + criado.id())).body(criado);
    }

    @PostMapping("/{id}/pagar")
    public ResponseEntity<PedidoResponse> pagar(
            @PathVariable Long id,
            @RequestParam(required = false) Long solicitanteId) {
        return ResponseEntity.ok(pedidoService.pagar(id, solicitanteId));
    }

    @PostMapping("/{id}/cancelar")
    public ResponseEntity<PedidoResponse> cancelar(
            @PathVariable Long id,
            @RequestParam(required = false) Long solicitanteId) {
        return ResponseEntity.ok(pedidoService.cancelar(id, solicitanteId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(
            @PathVariable Long id,
            @RequestParam(required = false) Long solicitanteId) {
        pedidoService.excluir(id, solicitanteId);
        return ResponseEntity.noContent().build();
    }
}
