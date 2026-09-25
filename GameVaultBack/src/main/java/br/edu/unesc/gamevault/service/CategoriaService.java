package br.edu.unesc.gamevault.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.edu.unesc.gamevault.dto.request.CategoriaRequest;
import br.edu.unesc.gamevault.dto.response.CategoriaResponse;
import br.edu.unesc.gamevault.entity.Categoria;
import br.edu.unesc.gamevault.exception.RecursoDuplicadoException;
import br.edu.unesc.gamevault.exception.RecursoNaoEncontradoException;
import br.edu.unesc.gamevault.mapper.CategoriaMapper;
import br.edu.unesc.gamevault.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoriaService {
    private final CategoriaRepository categoriaRepository;
    private final CategoriaMapper categoriaMapper;

    @Transactional(readOnly = true)
    public Page<CategoriaResponse> listar(String nome, Pageable paginacao) {
        Page<Categoria> pagina = (nome == null || nome.isBlank())
                ? categoriaRepository.findAll(paginacao)
                : categoriaRepository.findByNomeContainingIgnoreCase(nome.trim(), paginacao);

        return pagina.map(categoriaMapper::paraResposta);
    }

    @Transactional(readOnly = true)
    public CategoriaResponse buscarPorId(Long id) {
        return categoriaMapper.paraResposta(buscarEntidade(id));
    }

    @Transactional
    public CategoriaResponse criar(CategoriaRequest requisicao) {
        String nome = requisicao.nome().trim();

        if (categoriaRepository.existsByNomeIgnoreCase(nome)) {
            throw new RecursoDuplicadoException("Já existe uma categoria com o nome '%s'".formatted(nome));
        }

        Categoria categoria = categoriaRepository.save(categoriaMapper.paraEntidade(requisicao));
        log.info("Categoria criada: id={} nome={}", categoria.getId(), categoria.getNome());

        return categoriaMapper.paraResposta(categoria);
    }

    @Transactional
    public CategoriaResponse atualizar(Long id, CategoriaRequest requisicao) {
        Categoria categoria = buscarEntidade(id);
        String nome = requisicao.nome().trim();

        if (categoriaRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new RecursoDuplicadoException("Já existe uma categoria com o nome '%s'".formatted(nome));
        }

        categoriaMapper.copiarParaEntidade(requisicao, categoria);
        log.info("Categoria atualizada: id={}", id);

        return categoriaMapper.paraResposta(categoria);
    }

    @Transactional
    public void excluir(Long id) {
        Categoria categoria = buscarEntidade(id);
        categoriaRepository.delete(categoria);
        log.info("Categoria excluída: id={}", id);
    }

    @Transactional(readOnly = true)
    public Categoria buscarEntidade(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria", id));
    }
}
