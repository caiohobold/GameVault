package br.edu.unesc.gamevault.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.edu.unesc.gamevault.dto.response.DeteccaoFraudeResponse;
import br.edu.unesc.gamevault.dto.response.MetricaModeloResponse;
import br.edu.unesc.gamevault.dto.response.ReindexacaoResponse;
import br.edu.unesc.gamevault.dto.response.ResumoAvaliacoesResponse;
import br.edu.unesc.gamevault.dto.response.TreinoDemandaResponse;
import br.edu.unesc.gamevault.dto.response.TreinoRecomendacaoResponse;
import br.edu.unesc.gamevault.dto.response.UsoIaResponse;
import br.edu.unesc.gamevault.ia.MetricaModeloService;
import br.edu.unesc.gamevault.ia.UsoIaService;
import br.edu.unesc.gamevault.ia.busca.EmbeddingService;
import br.edu.unesc.gamevault.ia.fraude.DeteccaoFraudeService;
import br.edu.unesc.gamevault.ia.promocao.SimulacaoPromocaoService;
import br.edu.unesc.gamevault.ia.recomendacao.RecomendacaoService;
import br.edu.unesc.gamevault.ia.resumo.ResumoAvaliacoesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "IA - Administração",
        description = "Uso e custo do Claude, métricas dos modelos e disparo manual dos treinos (os treinos também rodam todo dia de madrugada).")
@RestController
@RequestMapping("/admin/ia")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminIaController {
    private final UsoIaService usoIaService;
    private final MetricaModeloService metricaService;
    private final EmbeddingService embeddingService;
    private final RecomendacaoService recomendacaoService;
    private final SimulacaoPromocaoService simulacaoService;
    private final DeteccaoFraudeService deteccaoService;
    private final ResumoAvaliacoesService resumoService;

    @Operation(summary = "Uso do Claude por módulo e por dia", description = "Chamadas, falhas, tokens e custo estimado.")
    @GetMapping("/uso")
    public ResponseEntity<List<UsoIaResponse>> uso(
            @Parameter(description = "Quantos dias para trás", example = "30") @RequestParam(defaultValue = "30") int dias) {
        return ResponseEntity.ok(usoIaService.resumir(dias));
    }

    @Operation(summary = "Métricas do último treino de cada modelo",
            description = "hit rate@10 da recomendação e erro médio absoluto (MAE) da previsão de demanda.")
    @GetMapping("/metricas")
    public ResponseEntity<List<MetricaModeloResponse>> metricas() {
        return ResponseEntity.ok(metricaService.listarMaisRecentes());
    }

    @Operation(summary = "Recalcula os embeddings de todos os jogos")
    @ApiResponse(responseCode = "503", description = "GameVaultML indisponível")
    @PostMapping("/reindexar")
    public ResponseEntity<ReindexacaoResponse> reindexar() {
        return ResponseEntity.ok(new ReindexacaoResponse(embeddingService.reindexarTodos()));
    }

    @Operation(summary = "Treina agora o modelo de recomendação")
    @PostMapping("/treinar/recomendacoes")
    public ResponseEntity<TreinoRecomendacaoResponse> treinarRecomendacoes() {
        return ResponseEntity.ok(recomendacaoService.treinar());
    }

    @Operation(summary = "Treina agora o modelo de previsão de demanda")
    @PostMapping("/treinar/demanda")
    public ResponseEntity<TreinoDemandaResponse> treinarDemanda() {
        return ResponseEntity.ok(simulacaoService.treinar());
    }

    @Operation(summary = "Roda agora a detecção de fraude")
    @PostMapping("/fraudes/detectar")
    public ResponseEntity<DeteccaoFraudeResponse> detectarFraudes() {
        return ResponseEntity.ok(deteccaoService.detectar());
    }

    @Operation(summary = "Gera agora o resumo das avaliações de um jogo")
    @ApiResponse(responseCode = "422", description = "O jogo tem menos de 3 avaliações aprovadas")
    @ApiResponse(responseCode = "503", description = "IA indisponível")
    @PostMapping("/resumos/{jogoId}")
    public ResponseEntity<ResumoAvaliacoesResponse> gerarResumo(@PathVariable Long jogoId) {
        return ResponseEntity.ok(resumoService.gerar(jogoId));
    }
}
