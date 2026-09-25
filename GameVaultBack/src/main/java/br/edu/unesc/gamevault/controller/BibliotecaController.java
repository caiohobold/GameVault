package br.edu.unesc.gamevault.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.unesc.gamevault.dto.request.BibliotecaHorasRequest;
import br.edu.unesc.gamevault.dto.response.BibliotecaResponse;
import br.edu.unesc.gamevault.service.BibliotecaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/biblioteca")
@RequiredArgsConstructor
public class BibliotecaController {
    private final BibliotecaService bibliotecaService;

    @GetMapping("/minha")
    public ResponseEntity<Page<BibliotecaResponse>> listarMinha(
            @RequestParam Long usuarioId,
            @PageableDefault(size = 20, sort = "dataAquisicao", direction = Sort.Direction.DESC) Pageable paginacao) {
        return ResponseEntity.ok(bibliotecaService.listarDoUsuario(usuarioId, paginacao));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BibliotecaResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(bibliotecaService.buscarPorId(id));
    }

    @PatchMapping("/{id}/horas")
    public ResponseEntity<BibliotecaResponse> atualizarHoras(
            @PathVariable Long id,
            @RequestParam(required = false) Long solicitanteId,
            @Valid @RequestBody BibliotecaHorasRequest requisicao) {
        return ResponseEntity.ok(bibliotecaService.atualizarHoras(id, requisicao, solicitanteId));
    }
}
