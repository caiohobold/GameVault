package br.edu.unesc.gamevault.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.unesc.gamevault.dto.response.BuscaSemanticaResponse;
import br.edu.unesc.gamevault.ia.busca.BuscaSemanticaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "IA - Busca", description = "Busca de jogos descrevendo o que se quer jogar, em linguagem natural.")
@RestController
@RequestMapping("/jogos")
@RequiredArgsConstructor
public class BuscaSemanticaController {
    private final BuscaSemanticaService buscaSemanticaService;

    @Operation(summary = "Busca jogos pelo significado da frase",
            description = """
                    Rota pública. O Claude separa a frase em texto e filtros (preço máximo, categorias,
                    promoção) e o texto é comparado com o embedding de cada jogo. Os resultados vêm em
                    ordem de relevância. Se o serviço de embeddings estiver fora do ar, a busca cai na
                    busca por título e o campo `modo` vem como `TEXTUAL`.
                    """)
    @ApiResponse(responseCode = "200", description = "Jogos encontrados e filtros entendidos")
    @SecurityRequirements
    @GetMapping("/busca")
    public ResponseEntity<BuscaSemanticaResponse> buscar(
            @Parameter(description = "O que o jogador procura", example = "terror pra jogar com amigos até 60 reais")
            @RequestParam("q") String consulta,
            @PageableDefault(size = 20) Pageable paginacao) {
        return ResponseEntity.ok(buscaSemanticaService.buscar(consulta, paginacao));
    }
}
