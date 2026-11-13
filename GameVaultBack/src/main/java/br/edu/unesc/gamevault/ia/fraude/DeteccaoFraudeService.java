package br.edu.unesc.gamevault.ia.fraude;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import br.edu.unesc.gamevault.dto.request.RevisaoAlertaRequest;
import br.edu.unesc.gamevault.dto.response.AlertaFraudeResponse;
import br.edu.unesc.gamevault.dto.response.DeteccaoFraudeResponse;
import br.edu.unesc.gamevault.entity.AlertaFraude;
import br.edu.unesc.gamevault.entity.enums.StatusAlertaFraude;
import br.edu.unesc.gamevault.exception.IaIndisponivelException;
import br.edu.unesc.gamevault.exception.RecursoNaoEncontradoException;
import br.edu.unesc.gamevault.exception.RegraDeNegocioException;
import br.edu.unesc.gamevault.ia.ml.ClienteML;
import br.edu.unesc.gamevault.ia.ml.ContratosMl.DeteccaoAnomaliasResposta;
import br.edu.unesc.gamevault.ia.ml.ContratosMl.UsuarioSuspeitoMl;
import br.edu.unesc.gamevault.mapper.UsuarioMapper;
import br.edu.unesc.gamevault.repository.AlertaFraudeRepository;
import br.edu.unesc.gamevault.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Detecção de fraude (módulo 7). O GameVaultML compara o comportamento de cada usuário
 * com o padrão da loja (Isolation Forest e, quando houver rótulos, um classificador
 * supervisionado). Aqui os suspeitos viram alertas. A IA só aponta: confirmar, descartar
 * ou desativar a conta é sempre decisão de um administrador.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeteccaoFraudeService {
    private static final int DIAS_DE_SILENCIO_APOS_DESCARTE = 30;

    private final ClienteML clienteML;
    private final AlertaFraudeRepository alertaRepository;
    private final UsuarioService usuarioService;
    private final UsuarioMapper usuarioMapper;
    private final TransactionTemplate transacao;

    public DeteccaoFraudeResponse detectar() {
        DeteccaoAnomaliasResposta resultado = clienteML.detectarAnomalias();

        Integer criados = transacao.execute(status -> {
            int novos = 0;
            for (UsuarioSuspeitoMl suspeito : resultado.suspeitos()) {
                if (!precisaDeAlerta(suspeito.usuarioId())) {
                    continue;
                }

                alertaRepository.save(AlertaFraude.builder()
                        .usuario(usuarioService.buscarEntidade(suspeito.usuarioId()))
                        .pontuacao(Math.max(0, Math.min(1, suspeito.pontuacao())))
                        .motivos(suspeito.motivos() == null ? List.of() : suspeito.motivos())
                        .status(StatusAlertaFraude.ABERTO)
                        .criadoEm(LocalDateTime.now())
                        .build());
                novos++;
            }
            return novos;
        });

        log.info("Deteccao de fraude: {} usuarios analisados, {} suspeitos, {} alertas novos",
                resultado.usuariosAnalisados(), resultado.suspeitos().size(), criados);

        return new DeteccaoFraudeResponse(resultado.usuariosAnalisados(), resultado.suspeitos().size(), criados,
                resultado.classificadorSupervisionado());
    }

    /**
     * Não repete alerta para quem já tem um aberto ou confirmado. Quem foi descartado pelo
     * admin só volta a ser alertado depois de um tempo, se continuar fora do padrão.
     */
    private boolean precisaDeAlerta(Long usuarioId) {
        if (alertaRepository.existsByUsuario_IdAndStatusIn(usuarioId,
                List.of(StatusAlertaFraude.ABERTO, StatusAlertaFraude.CONFIRMADO))) {
            return false;
        }
        return !alertaRepository.existsByUsuario_IdAndStatusAndRevisadoEmAfter(usuarioId,
                StatusAlertaFraude.DESCARTADO, LocalDateTime.now().minusDays(DIAS_DE_SILENCIO_APOS_DESCARTE));
    }

    @Scheduled(cron = "${gamevault.ia.fraude.cron}")
    public void detectarAgendado() {
        try {
            detectar();
        } catch (IaIndisponivelException excecao) {
            log.warn("Deteccao de fraude agendada nao rodou: {}", excecao.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<AlertaFraudeResponse> listar(StatusAlertaFraude status, Pageable paginacao) {
        StatusAlertaFraude filtro = status == null ? StatusAlertaFraude.ABERTO : status;
        return alertaRepository.findByStatus(filtro, paginacao).map(this::paraResposta);
    }

    @Transactional
    public AlertaFraudeResponse revisar(Long id, RevisaoAlertaRequest requisicao, Long administradorId) {
        if (requisicao.status() == StatusAlertaFraude.ABERTO) {
            throw new RegraDeNegocioException("A revisão precisa confirmar ou descartar o alerta");
        }

        AlertaFraude alerta = alertaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Alerta de fraude", id));

        alerta.setStatus(requisicao.status());
        alerta.setRevisadoPor(usuarioService.buscarEntidade(administradorId));
        alerta.setRevisadoEm(LocalDateTime.now());

        log.info("Alerta de fraude {} revisado como {} pelo admin {}", id, requisicao.status(), administradorId);
        return paraResposta(alerta);
    }

    private AlertaFraudeResponse paraResposta(AlertaFraude alerta) {
        return new AlertaFraudeResponse(
                alerta.getId(),
                usuarioMapper.paraResumo(alerta.getUsuario()),
                alerta.getUsuario().getAtivo(),
                alerta.getUsuario().getDataCadastro(),
                alerta.getPontuacao(),
                alerta.getMotivos(),
                alerta.getStatus(),
                alerta.getCriadoEm(),
                alerta.getRevisadoPor() == null ? null : usuarioMapper.paraResumo(alerta.getRevisadoPor()),
                alerta.getRevisadoEm());
    }
}
