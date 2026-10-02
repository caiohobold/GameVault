package br.edu.unesc.gamevault.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.unesc.gamevault.dto.request.BibliotecaHorasRequest;
import br.edu.unesc.gamevault.dto.response.BibliotecaResponse;
import br.edu.unesc.gamevault.security.UsuarioAutenticado;
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
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @PageableDefault(size = 20, sort = "dataAquisicao", direction = Sort.Direction.DESC) Pageable paginacao) {
        return ResponseEntity.ok(bibliotecaService.listarDoUsuario(autenticado.getId(), paginacao));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BibliotecaResponse> buscarPorId(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        return ResponseEntity.ok(bibliotecaService.buscarPorIdDoUsuario(id, autenticado.getId()));
    }

    @PatchMapping("/{id}/horas")
    public ResponseEntity<BibliotecaResponse> atualizarHoras(
            @PathVariable Long id,
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Valid @RequestBody BibliotecaHorasRequest requisicao) {
        return ResponseEntity.ok(bibliotecaService.atualizarHoras(id, requisicao, autenticado.getId()));
    }
}
