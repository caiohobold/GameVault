package br.edu.unesc.gamevault.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(

        @NotBlank(message = "O nome da categoria é obrigatório")
        @Size(max = 60, message = "O nome da categoria deve ter no máximo 60 caracteres")
        String nome,

        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres")
        String descricao) {
}
