package br.edu.unesc.gamevault.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Título e tópicos soltos para a IA escrever a descrição do jogo")
public record DescricaoJogoRequest(
        @NotBlank(message = "O título é obrigatório")
        @Size(max = 150, message = "O título deve ter no máximo 150 caracteres")
        @Schema(example = "Fenda Profunda")
        String titulo,
        @NotBlank(message = "Informe pelo menos um tópico sobre o jogo")
        @Size(max = 1000, message = "Os tópicos devem ter no máximo 1000 caracteres")
        @Schema(example = "roguelike, pixel art, cavernas geradas a cada partida, 2 jogadores, chefões difíceis")
        String topicos) {
}
