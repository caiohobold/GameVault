package br.edu.unesc.gamevault.dto.response;

import br.edu.unesc.gamevault.ia.cadastro.IdiomaTraducao;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Descrição traduzida pela IA")
public record TraducaoResponse(IdiomaTraducao idioma, String texto) {
}
