package br.edu.unesc.gamevault.ia.claude;

import com.anthropic.models.messages.OutputConfig;

/**
 * Cada uso do Claude no sistema, com o arquivo de prompt e o esforço de raciocínio.
 * Classificações curtas usam esforço baixo; textos para o usuário e o chat, médio.
 */
public enum ModuloIa {
    MODERACAO("moderacao", OutputConfig.Effort.LOW),
    FILTROS_BUSCA("filtros-busca", OutputConfig.Effort.LOW),
    RESUMO_AVALIACOES("resumo-avaliacoes", OutputConfig.Effort.MEDIUM),
    DESCRICAO_JOGO("descricao-jogo", OutputConfig.Effort.MEDIUM),
    CATEGORIAS_JOGO("categorias-jogo", OutputConfig.Effort.LOW),
    TRADUCAO("traducao", OutputConfig.Effort.LOW),
    ASSISTENTE_CHAT("assistente-chat", OutputConfig.Effort.MEDIUM);

    private final String arquivoPrompt;
    private final OutputConfig.Effort esforco;

    ModuloIa(String arquivoPrompt, OutputConfig.Effort esforco) {
        this.arquivoPrompt = arquivoPrompt;
        this.esforco = esforco;
    }

    public String arquivoPrompt() {
        return arquivoPrompt;
    }

    public OutputConfig.Effort esforco() {
        return esforco;
    }
}
