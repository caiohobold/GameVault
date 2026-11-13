package br.edu.unesc.gamevault.dto.request;

import br.edu.unesc.gamevault.ia.cadastro.IdiomaTraducao;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Descrição a ser traduzida")
public record TraducaoRequest(
        @NotBlank(message = "O texto é obrigatório")
        @Size(max = 2000, message = "O texto deve ter no máximo 2000 caracteres")
        String texto,
        @NotNull(message = "O idioma é obrigatório")
        @Schema(description = "EN (inglês) ou ES (espanhol)", example = "EN")
        IdiomaTraducao idioma) {
}
