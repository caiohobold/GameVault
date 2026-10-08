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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "Autenticação", description = "Cadastro e login. São as únicas rotas que não exigem token.")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @Operation(
            summary = "Cria uma conta e já devolve o token",
            description = """
                    Aceita apenas os papéis `USUARIO` (padrão quando o campo é omitido) e `PUBLICADORA`.
                    Criar um `ADMIN` por aqui é recusado com 422: seria escalonamento de privilégio.
                    A resposta já traz um token válido, então não é preciso chamar o login em seguida.
                    """)
    @ApiResponse(responseCode = "201", description = "Conta criada e autenticada")
    @ApiResponse(responseCode = "400", description = "Campos inválidos",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErroResposta")))
    @ApiResponse(responseCode = "409", description = "Já existe conta com este e-mail",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErroResposta")))
    @ApiResponse(responseCode = "422", description = "Tentativa de criar conta ADMIN por auto-cadastro",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErroResposta")))
    @SecurityRequirements
    @PostMapping("/registrar")
    public ResponseEntity<LoginResponse> registrar(@Valid @RequestBody RegistroRequest requisicao) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(requisicao));
    }

    @Operation(
            summary = "Autentica e devolve o token JWT",
            description = """
                    Use o token devolvido no botão **Authorize** desta página, ou no cabeçalho
                    `Authorization: Bearer {token}`. O token vale 24 horas.
                    """)
    @ApiResponse(responseCode = "200", description = "Autenticado")
    @ApiResponse(responseCode = "401", description = "E-mail ou senha incorretos",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErroResposta")))
    @ApiResponse(responseCode = "403", description = "Conta desativada",
            content = @Content(schema = @Schema(ref = "#/components/schemas/ErroResposta")))
    @SecurityRequirements
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest requisicao) {
        return ResponseEntity.ok(authService.autenticar(requisicao));
    }
}
