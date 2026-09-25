package br.edu.unesc.gamevault.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.unesc.gamevault.dto.request.PromocaoRequest;
import br.edu.unesc.gamevault.dto.response.PromocaoResponse;
import br.edu.unesc.gamevault.entity.Jogo;
import br.edu.unesc.gamevault.entity.Promocao;
import br.edu.unesc.gamevault.entity.Usuario;
import br.edu.unesc.gamevault.entity.enums.Role;
import br.edu.unesc.gamevault.exception.AcessoNegadoException;
import br.edu.unesc.gamevault.exception.RecursoNaoEncontradoException;
import br.edu.unesc.gamevault.exception.RegraDeNegocioException;
import br.edu.unesc.gamevault.mapper.PromocaoMapper;
import br.edu.unesc.gamevault.repository.PromocaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PromocaoService {
    private final PromocaoRepository promocaoRepository;
    private final JogoService jogoService;
    private final UsuarioService usuarioService;
    private final PromocaoMapper promocaoMapper;

    @Transactional(readOnly = true)
    public Page<PromocaoResponse> listarPorJogo(Long jogoId, Pageable paginacao) {
        jogoService.buscarEntidade(jogoId);

        return promocaoRepository.findByJogo_Id(jogoId, paginacao).map(promocaoMapper::paraResposta);
    }

    @Transactional(readOnly = true)
    public Page<PromocaoResponse> listarTodas(Pageable paginacao) {
        return promocaoRepository.findAll(paginacao).map(promocaoMapper::paraResposta);
    }

    @Transactional(readOnly = true)
    public PromocaoResponse buscarPorId(Long id) {
        return promocaoMapper.paraResposta(buscarEntidade(id));
    }

    @Transactional
    public PromocaoResponse criar(PromocaoRequest requisicao, Long solicitanteId) {
        Jogo jogo = jogoService.buscarEntidade(requisicao.jogoId());
        validarDonoDoJogo(jogo, solicitanteId);
        validarPeriodo(requisicao);

        Promocao promocao = Promocao.builder()
                .jogo(jogo)
                .percentualDesconto(requisicao.percentualDesconto())
                .dataInicio(requisicao.dataInicio())
                .dataFim(requisicao.dataFim())
                .ativa(requisicao.ativa() == null ? Boolean.TRUE : requisicao.ativa())
                .build();

        promocao = promocaoRepository.save(promocao);
        log.info("Promoção criada: id={} jogo={} desconto={}%", promocao.getId(), jogo.getId(),
                promocao.getPercentualDesconto());

        return promocaoMapper.paraResposta(promocao);
    }

    @Transactional
    public PromocaoResponse atualizar(Long id, PromocaoRequest requisicao, Long solicitanteId) {
        Promocao promocao = buscarEntidade(id);
        validarDonoDoJogo(promocao.getJogo(), solicitanteId);
        validarPeriodo(requisicao);

        Jogo jogo = jogoService.buscarEntidade(requisicao.jogoId());
        validarDonoDoJogo(jogo, solicitanteId);

        promocao.setJogo(jogo);
        promocao.setPercentualDesconto(requisicao.percentualDesconto());
        promocao.setDataInicio(requisicao.dataInicio());
        promocao.setDataFim(requisicao.dataFim());

        if (requisicao.ativa() != null) {
            promocao.setAtiva(requisicao.ativa());
        }

        log.info("Promoção atualizada: id={}", id);
        return promocaoMapper.paraResposta(promocao);
    }

    @Transactional
    public void excluir(Long id, Long solicitanteId) {
        Promocao promocao = buscarEntidade(id);
        validarDonoDoJogo(promocao.getJogo(), solicitanteId);

        promocaoRepository.delete(promocao);
        log.info("Promoção excluída: id={}", id);
    }

    @Transactional(readOnly = true)
    public BigDecimal calcularDesconto(Jogo jogo, LocalDateTime momento) {
        Optional<Promocao> vigente = promocaoRepository.buscarVigente(jogo.getId(), momento);

        if (vigente.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal percentual = BigDecimal.valueOf(vigente.get().getPercentualDesconto());

        return jogo.getPreco()
                .multiply(percentual)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public Promocao buscarEntidade(Long id) {
        return promocaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Promoção", id));
    }

    private void validarPeriodo(PromocaoRequest requisicao) {
        if (!requisicao.dataFim().isAfter(requisicao.dataInicio())) {
            throw new RegraDeNegocioException(
                    "A data de fim da promoção precisa ser posterior à data de início");
        }
    }

    private void validarDonoDoJogo(Jogo jogo, Long solicitanteId) {
        if (solicitanteId == null) {
            return;
        }

        Usuario solicitante = usuarioService.buscarEntidade(solicitanteId);

        if (solicitante.getRole() == Role.ADMIN) {
            return;
        }

        if (!jogo.getPublicadora().getId().equals(solicitanteId)) {
            throw new AcessoNegadoException(
                    "O usuário %d não é a publicadora do jogo '%s'".formatted(solicitanteId, jogo.getTitulo()));
        }
    }
}
