package br.edu.unesc.gamevault.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.unesc.gamevault.dto.response.ResumoAvaliacoesResponse;
import br.edu.unesc.gamevault.ia.resumo.ResumoAvaliacoesService;
import br.edu.unesc.gamevault.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "IA - Resumo das avaliações", description = "O que os jogadores dizem de cada jogo, resumido pelo Claude.")
@RestController
@RequestMapping("/jogos")
@RequiredArgsConstructor
public class ResumoAvaliacoesController {
    private final ResumoAvaliacoesService resumoService;

    @Operation(summary = "Resumo das avaliações de um jogo",
            description = """
                    Rota pública. O resumo é gerado quando o jogo tem pelo menos 3 avaliações aprovadas e
                    refeito a cada 10 avaliações novas. Devolve 404 enquanto o jogo não tiver resumo.
                    """)
    @ApiResponse(responseCode = "200", description = "Resumo, pontos positivos e negativos e sentimento por aspecto")
    @ApiResponse(responseCode = "404", description = "Jogo inexistente ou ainda sem resumo")
    @SecurityRequirements
    @GetMapping("/{id}/avaliacoes/resumo")
    public ResponseEntity<ResumoAvaliacoesResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(resumoService.buscar(id));
    }

    @Operation(summary = "Temas das avaliações dos jogos da publicadora autenticada",
            description = "Um resumo por jogo da publicadora que já tem resumo gerado.")
    @ApiResponse(responseCode = "200", description = "Resumos dos próprios jogos")
    @GetMapping("/meus/avaliacoes/temas")
    @PreAuthorize("hasAnyRole('ADMIN', 'PUBLICADORA')")
    public ResponseEntity<List<ResumoAvaliacoesResponse>> listarTemas(
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        return ResponseEntity.ok(resumoService.listarDaPublicadora(autenticado.getId()));
    }
}
