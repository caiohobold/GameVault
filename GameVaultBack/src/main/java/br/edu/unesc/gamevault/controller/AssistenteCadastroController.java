package br.edu.unesc.gamevault.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.edu.unesc.gamevault.dto.request.CategoriasJogoRequest;
import br.edu.unesc.gamevault.dto.request.DescricaoJogoRequest;
import br.edu.unesc.gamevault.dto.request.DuplicadosRequest;
import br.edu.unesc.gamevault.dto.request.TraducaoRequest;
import br.edu.unesc.gamevault.dto.response.CategoriasSugeridasResponse;
import br.edu.unesc.gamevault.dto.response.DescricaoGeradaResponse;
import br.edu.unesc.gamevault.dto.response.JogoDuplicadoResponse;
import br.edu.unesc.gamevault.dto.response.TraducaoResponse;
import br.edu.unesc.gamevault.ia.cadastro.AssistenteCadastroService;
import br.edu.unesc.gamevault.security.UsuarioAutenticado;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Tag(name = "IA - Assistente de cadastro",
        description = "Sugestões para a publicadora no cadastro de jogos. Nada é gravado: o jogo continua sendo salvo pelo POST /jogos.")
@RestController
@RequestMapping("/jogos/assistente")
@PreAuthorize("hasAnyRole('ADMIN', 'PUBLICADORA')")
@RequiredArgsConstructor
public class AssistenteCadastroController {
    private final AssistenteCadastroService assistenteService;

    @Operation(summary = "Escreve a descrição do jogo a partir de tópicos",
            description = "Limite de 20 usos por hora no assistente. A descrição gerada passa pela moderação.")
    @ApiResponse(responseCode = "200", description = "Descrição sugerida")
    @ApiResponse(responseCode = "429", description = "Limite de uso por hora atingido")
    @ApiResponse(responseCode = "503", description = "IA indisponível")
    @PostMapping("/descricao")
    public ResponseEntity<DescricaoGeradaResponse> gerarDescricao(
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Valid @RequestBody DescricaoJogoRequest requisicao) {
        return ResponseEntity.ok(assistenteService.gerarDescricao(requisicao, autenticado.getId()));
    }

    @Operation(summary = "Sugere categorias para o jogo", description = "Só categorias já cadastradas, no máximo 4.")
    @ApiResponse(responseCode = "200", description = "Categorias sugeridas")
    @PostMapping("/categorias")
    public ResponseEntity<CategoriasSugeridasResponse> sugerirCategorias(
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Valid @RequestBody CategoriasJogoRequest requisicao) {
        return ResponseEntity.ok(assistenteService.sugerirCategorias(requisicao, autenticado.getId()));
    }

    @Operation(summary = "Traduz a descrição para inglês ou espanhol")
    @ApiResponse(responseCode = "200", description = "Texto traduzido")
    @PostMapping("/traducao")
    public ResponseEntity<TraducaoResponse> traduzir(
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Valid @RequestBody TraducaoRequest requisicao) {
        return ResponseEntity.ok(assistenteService.traduzir(requisicao, autenticado.getId()));
    }

    @Operation(summary = "Procura jogos já cadastrados muito parecidos",
            description = "Compara o embedding do texto com os jogos ativos; similaridade a partir de 0,9.")
    @ApiResponse(responseCode = "200", description = "Possíveis duplicados (lista vazia quando não há)")
    @PostMapping("/duplicados")
    public ResponseEntity<List<JogoDuplicadoResponse>> procurarDuplicados(
            @AuthenticationPrincipal UsuarioAutenticado autenticado,
            @Valid @RequestBody DuplicadosRequest requisicao) {
        return ResponseEntity.ok(assistenteService.procurarDuplicados(requisicao, autenticado.getId()));
    }
}
