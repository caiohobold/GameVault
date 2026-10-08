package br.edu.unesc.gamevault.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para publicar ou editar um jogo")
public record JogoRequest(

        @NotBlank(message = "O título é obrigatório")
        @Size(max = 150, message = "O título deve ter no máximo 150 caracteres")
        @Schema(description = "Título do jogo", example = "Sombras de Aldoria")
        String titulo,

        @Size(max = 2000, message = "A descrição deve ter no máximo 2000 caracteres")
        @Schema(description = "Sinopse exibida na página do jogo", example = "Um RPG de mundo aberto onde cada escolha reescreve o destino do reino.")
        String descricao,

        @NotNull(message = "O preço é obrigatório")
        @DecimalMin(value = "0.0", message = "O preço não pode ser negativo")
        @Schema(description = "Preço de tabela, sem desconto", example = "199.90")
        BigDecimal preco,

        @Schema(description = "Data de lançamento", example = "2024-03-15")
        LocalDate dataLancamento,

        /**
         * Só é lido quando quem chama é ADMIN. Para uma PUBLICADORA o jogo é
         * sempre registrado em nome dela própria, independente do que vier aqui.
         */
        @Schema(description = "Só lido quando quem chama é ADMIN; a publicadora publica em nome próprio", example = "2")
        Long publicadoraId,

        @NotEmpty(message = "Informe ao menos uma categoria")
        @Schema(description = "Identificadores das categorias do jogo", example = "[1, 3]")
        Set<Long> categoriaIds) {
}
