package br.edu.unesc.gamevault.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.unesc.gamevault.dto.request.SimulacaoPromocaoRequest;
import br.edu.unesc.gamevault.dto.response.PrevisaoDemandaResponse;
import br.edu.unesc.gamevault.dto.response.SimulacaoPromocaoResponse;
import br.edu.unesc.gamevault.ia.promocao.SimulacaoPromocaoService;
import br.edu.unesc.gamevault.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "IA - Promoções", description = "Previsão de vendas e simulação de descontos pelo modelo de regressão.")
@RestController
@PreAuthorize("hasAnyRole('ADMIN', 'PUBLICADORA')")
@RequiredArgsConstructor
public class SimulacaoPromocaoController {
    private final SimulacaoPromocaoService simulacaoService;

    @Operation(summary = "Simula descontos de 0% a 90% para um período",
            description = """
                    Para cada percentual, o modelo prevê as vendas e a loja calcula a receita. O cenário
                    de maior receita vem marcado como sugerido. Só nos próprios jogos (RN09). A promoção
                    não é criada: isso continua sendo feito no POST /promocoes.
                    """)
    @ApiResponse(responseCode = "200", description = "Cenários e sugestão")
    @ApiResponse(responseCode = "403", description = "O jogo é de outra publicadora")
    @ApiResponse(responseCode = "503", description = "Serviço de aprendizado de máquina indisponível")
    @PostMapping("/promocoes/simular")
    public ResponseEntity<SimulacaoPromocaoResponse> simular(
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Valid @RequestBody SimulacaoPromocaoRequest requisicao) {
        return ResponseEntity.ok(simulacaoService.simular(requisicao, autenticado.getId()));
    }

    @Operation(summary = "Previsão de vendas das próximas semanas",
            description = "Considera as promoções já cadastradas. Só nos próprios jogos (RN09).")
    @ApiResponse(responseCode = "200", description = "Vendas previstas por semana")
    @GetMapping("/jogos/{id}/previsao-demanda")
    public ResponseEntity<PrevisaoDemandaResponse> preverDemanda(
            @PathVariable Long id,
            @Parameter(description = "Quantidade de semanas, de 1 a 12", example = "4")
            @RequestParam(defaultValue = "4") int semanas,
            @AuthenticationPrincipal UsuarioAutenticado autenticado) {
        return ResponseEntity.ok(simulacaoService.preverProximasSemanas(id, semanas, autenticado.getId()));
    }
}
