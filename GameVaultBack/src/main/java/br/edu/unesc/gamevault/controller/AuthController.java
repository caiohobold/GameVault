package br.edu.unesc.gamevault.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.unesc.gamevault.dto.request.LoginRequest;
import br.edu.unesc.gamevault.dto.request.RegistroRequest;
import br.edu.unesc.gamevault.dto.response.LoginResponse;
import br.edu.unesc.gamevault.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/registrar")
    public ResponseEntity<LoginResponse> registrar(@Valid @RequestBody RegistroRequest requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(requisicao));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest requisicao) {
        return ResponseEntity.ok(authService.autenticar(requisicao));
    }
}
