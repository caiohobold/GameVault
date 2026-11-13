package br.edu.unesc.gamevault.ia.moderacao;

import org.springframework.stereotype.Service;

import br.edu.unesc.gamevault.entity.enums.StatusModeracao;
import br.edu.unesc.gamevault.exception.IaIndisponivelException;
import br.edu.unesc.gamevault.ia.claude.ClienteClaude;
import br.edu.unesc.gamevault.ia.claude.ModuloIa;
import br.edu.unesc.gamevault.ia.claude.TextoSeguro;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Moderação de comentários pelo Claude (módulo 3). Avaliação sem comentário é aprovada
 * direto. Se a IA falhar, o comentário vai para revisão humana: nada é publicado sem
 * passar pela IA ou por um administrador.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModeracaoService {
    private final ClienteClaude clienteClaude;

    public ResultadoModeracao moderarComentario(String tituloJogo, Integer nota, String comentario, Long usuarioId) {
        if (comentario == null || comentario.isBlank()) {
            return ResultadoModeracao.aprovadaSemComentario();
        }

        String mensagem = TextoSeguro.tag("jogo", tituloJogo) + "\n"
                + TextoSeguro.tag("nota", String.valueOf(nota)) + "\n"
                + TextoSeguro.tag("comentario", comentario);

        return classificar(mensagem, usuarioId);
    }

    /**
     * Modera um texto que vai ao ar sem ser comentário, como a descrição gerada pelo
     * assistente de cadastro.
     */
    public ResultadoModeracao moderarTexto(String titulo, String texto, Long usuarioId) {
        String mensagem = TextoSeguro.tag("jogo", titulo) + "\n"
                + TextoSeguro.tag("nota", "sem nota: é a descrição do jogo escrita pela publicadora") + "\n"
                + TextoSeguro.tag("comentario", texto);

        return classificar(mensagem, usuarioId);
    }

    private ResultadoModeracao classificar(String mensagem, Long usuarioId) {
        try {
            ResultadoModeracao resultado = clienteClaude.gerarEstruturado(ModuloIa.MODERACAO, mensagem,
                    ResultadoModeracao.class, usuarioId);
            return normalizar(resultado);
        } catch (IaIndisponivelException excecao) {
            log.info("Moderacao automatica indisponivel, comentario vai para revisao humana: {}",
                    excecao.getMessage());
            return ResultadoModeracao.paraRevisaoHumana();
        }
    }

    private ResultadoModeracao normalizar(ResultadoModeracao resultado) {
        StatusModeracao decisao = resultado.decisao() == null ? StatusModeracao.EM_REVISAO : resultado.decisao();
        String motivo = resultado.motivo() == null || resultado.motivo().isBlank()
                ? "Comentário analisado pela moderação automática"
                : resultado.motivo().strip();

        if (motivo.length() > 500) {
            motivo = motivo.substring(0, 500);
        }

        return new ResultadoModeracao(decisao, resultado.contemSpoiler(), motivo);
    }
}
