package br.edu.unesc.gamevault.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.unesc.gamevault.dto.response.RecomendacaoResponse;
import br.edu.unesc.gamevault.ia.recomendacao.RecomendacaoService;
import br.edu.unesc.gamevault.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "IA - Recomendação", description = "Jogos sugeridos por filtragem colaborativa e semelhança de conteúdo.")
@RestController
@RequiredArgsConstructor
public class RecomendacaoController {
    private final RecomendacaoService recomendacaoService;

    @Operation(summary = "Recomendações para o usuário autenticado",
            description = """
                    Vem do modelo de filtragem colaborativa treinado todo dia. Jogos que o usuário já
                    tem ou que foram desativados nunca aparecem. Para quem ainda não comprou nada, usa
                    a semelhança com os jogos que ele favoritou ou visitou. Lista vazia quando não há
                    dados suficientes.
                    """)
    @ApiResponse(responseCode = "200", description = "Jogos recomendados, do mais ao menos indicado")
    @GetMapping("/recomendacoes/minhas")
    public ResponseEntity<List<RecomendacaoResponse>> listarMinhas(
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Parameter(description = "Quantidade máxima, de 1 a 20", example = "10")
            @RequestParam(defaultValue = "10") int limite) {
        return ResponseEntity.ok(recomendacaoService.listarDoUsuario(autenticado.getId(), limite));
    }

    @Operation(summary = "Jogos parecidos com um jogo", description = "Rota pública.")
    @ApiResponse(responseCode = "200", description = "Jogos similares")
    @SecurityRequirements
    @GetMapping("/jogos/{id}/similares")
    public ResponseEntity<List<RecomendacaoResponse>> listarSimilares(
            @PathVariable Long id,
            @RequestParam(defaultValue = "6") int limite) {
        return ResponseEntity.ok(recomendacaoService.listarSimilares(id, limite));
    }
}
