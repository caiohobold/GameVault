package br.edu.unesc.gamevault.repository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import lombok.RequiredArgsConstructor;

/**
 * Acesso à tabela {@code jogo_embedding} com SQL nativo do pgvector. Não é uma
 * entidade JPA porque o Hibernate não conhece o tipo {@code vector}.
 *
 * <p>O operador {@code <=>} é a distância de cosseno; similaridade = 1 - distância.
 */
@Repository
@RequiredArgsConstructor
public class JogoEmbeddingRepository {
    private final NamedParameterJdbcTemplate jdbc;

    public record JogoSimilaridade(Long jogoId, double similaridade) {
    }

    /** Filtros opcionais da busca; nulos ou vazios são ignorados. */
    public record FiltrosSql(BigDecimal precoMaximo, Set<Long> categoriaIds, boolean apenasEmPromocao) {
    }

    public Optional<String> buscarHash(Long jogoId) {
        List<String> hashes = jdbc.queryForList(
                "select texto_hash from jogo_embedding where jogo_id = :jogoId",
                new MapSqlParameterSource("jogoId", jogoId), String.class);
        return hashes.stream().findFirst();
    }

    public void salvar(Long jogoId, float[] vetor, String hash) {
        jdbc.update("""
                insert into jogo_embedding (jogo_id, embedding, texto_hash, gerado_em)
                values (:jogoId, cast(:vetor as vector), :hash, :agora)
                on conflict (jogo_id) do update
                   set embedding = excluded.embedding,
                       texto_hash = excluded.texto_hash,
                       gerado_em = excluded.gerado_em
                """, new MapSqlParameterSource()
                .addValue("jogoId", jogoId)
                .addValue("vetor", paraTexto(vetor))
                .addValue("hash", hash)
                .addValue("agora", Timestamp.valueOf(LocalDateTime.now())));
    }

    public List<Long> idsDeJogosSemEmbedding() {
        return jdbc.queryForList("""
                select j.id from jogo j
                 where not exists (select 1 from jogo_embedding e where e.jogo_id = j.id)
                 order by j.id
                """, new MapSqlParameterSource(), Long.class);
    }

    public long contar() {
        Long total = jdbc.queryForObject("select count(*) from jogo_embedding", new MapSqlParameterSource(),
                Long.class);
        return total == null ? 0 : total;
    }

    /** Jogos ativos mais próximos do vetor, já com os filtros da busca aplicados. */
    public List<JogoSimilaridade> buscarPorSimilaridade(float[] vetor, FiltrosSql filtros, int limite,
            long deslocamento) {
        MapSqlParameterSource parametros = parametrosDeFiltro(filtros)
                .addValue("vetor", paraTexto(vetor))
                .addValue("limite", limite)
                .addValue("deslocamento", deslocamento);

        String sql = """
                select j.id as jogo_id, 1 - (e.embedding <=> cast(:vetor as vector)) as similaridade
                """ + fromComFiltros(filtros) + """
                 order by e.embedding <=> cast(:vetor as vector)
                 limit :limite offset :deslocamento
                """;

        return jdbc.query(sql, parametros,
                (linha, indice) -> new JogoSimilaridade(linha.getLong("jogo_id"), linha.getDouble("similaridade")));
    }

    public long contarComFiltros(FiltrosSql filtros) {
        Long total = jdbc.queryForObject("select count(*) " + fromComFiltros(filtros), parametrosDeFiltro(filtros),
                Long.class);
        return total == null ? 0 : total;
    }

    /** Jogos ativos mais parecidos com um jogo, pelo texto. */
    public List<JogoSimilaridade> vizinhosDoJogo(Long jogoId, int limite) {
        return jdbc.query("""
                select e2.jogo_id, 1 - (e2.embedding <=> e1.embedding) as similaridade
                  from jogo_embedding e1
                  join jogo_embedding e2 on e2.jogo_id <> e1.jogo_id
                  join jogo j on j.id = e2.jogo_id and j.ativo = true
                 where e1.jogo_id = :jogoId
                 order by e2.embedding <=> e1.embedding
                 limit :limite
                """, new MapSqlParameterSource().addValue("jogoId", jogoId).addValue("limite", limite),
                (linha, indice) -> new JogoSimilaridade(linha.getLong("jogo_id"), linha.getDouble("similaridade")));
    }

    /**
     * Jogos ativos mais próximos do "centro" de um conjunto de jogos (média dos vetores).
     * Usado para recomendar a quem ainda não comprou nada.
     */
    public List<JogoSimilaridade> vizinhosDoCentro(Collection<Long> jogosDeReferencia, Collection<Long> excluir,
            int limite) {
        if (jogosDeReferencia.isEmpty()) {
            return List.of();
        }

        List<Long> excluidos = new ArrayList<>(excluir);
        excluidos.add(-1L);

        return jdbc.query("""
                with centro as (select avg(embedding) as vetor from jogo_embedding where jogo_id in (:referencias))
                select e.jogo_id, 1 - (e.embedding <=> centro.vetor) as similaridade
                  from jogo_embedding e
                  join jogo j on j.id = e.jogo_id and j.ativo = true
                 cross join centro
                 where centro.vetor is not null
                   and e.jogo_id not in (:excluidos)
                 order by e.embedding <=> centro.vetor
                 limit :limite
                """, new MapSqlParameterSource()
                .addValue("referencias", jogosDeReferencia)
                .addValue("excluidos", excluidos)
                .addValue("limite", limite),
                (linha, indice) -> new JogoSimilaridade(linha.getLong("jogo_id"), linha.getDouble("similaridade")));
    }

    /** Jogos ativos com similaridade mínima ao vetor (detecção de cadastro duplicado). */
    public List<JogoSimilaridade> semelhantesAoVetor(float[] vetor, double similaridadeMinima, Long ignorarJogoId,
            int limite) {
        return jdbc.query("""
                select e.jogo_id, 1 - (e.embedding <=> cast(:vetor as vector)) as similaridade
                  from jogo_embedding e
                  join jogo j on j.id = e.jogo_id and j.ativo = true
                 where 1 - (e.embedding <=> cast(:vetor as vector)) >= :minimo
                   and e.jogo_id <> :ignorar
                 order by e.embedding <=> cast(:vetor as vector)
                 limit :limite
                """, new MapSqlParameterSource()
                .addValue("vetor", paraTexto(vetor))
                .addValue("minimo", similaridadeMinima)
                .addValue("ignorar", ignorarJogoId == null ? -1L : ignorarJogoId)
                .addValue("limite", limite),
                (linha, indice) -> new JogoSimilaridade(linha.getLong("jogo_id"), linha.getDouble("similaridade")));
    }

    private String fromComFiltros(FiltrosSql filtros) {
        StringBuilder sql = new StringBuilder("""
                  from jogo j
                  join jogo_embedding e on e.jogo_id = j.id
                  left join lateral (
                        select max(p.percentual_desconto) as percentual
                          from promocao p
                         where p.jogo_id = j.id and p.ativa = true
                           and p.data_inicio <= :agora and p.data_fim > :agora
                  ) promo on true
                 where j.ativo = true
                """);

        if (filtros.precoMaximo() != null) {
            sql.append(" and j.preco * (100 - coalesce(promo.percentual, 0)) / 100 <= :precoMaximo\n");
        }
        if (filtros.apenasEmPromocao()) {
            sql.append(" and promo.percentual is not null\n");
        }
        if (filtros.categoriaIds() != null && !filtros.categoriaIds().isEmpty()) {
            sql.append("""
                     and exists (select 1 from jogo_categoria jc
                                  where jc.jogo_id = j.id and jc.categoria_id in (:categoriaIds))
                    """);
        }

        return sql.toString();
    }

    private MapSqlParameterSource parametrosDeFiltro(FiltrosSql filtros) {
        MapSqlParameterSource parametros = new MapSqlParameterSource()
                .addValue("agora", Timestamp.valueOf(LocalDateTime.now()));

        if (filtros.precoMaximo() != null) {
            parametros.addValue("precoMaximo", filtros.precoMaximo());
        }
        if (filtros.categoriaIds() != null && !filtros.categoriaIds().isEmpty()) {
            parametros.addValue("categoriaIds", filtros.categoriaIds());
        }

        return parametros;
    }

    /** O pgvector recebe o vetor como texto no formato {@code [0.1,0.2,...]}. */
    static String paraTexto(float[] vetor) {
        StringBuilder texto = new StringBuilder(vetor.length * 10).append('[');
        for (int i = 0; i < vetor.length; i++) {
            if (i > 0) {
                texto.append(',');
            }
            texto.append(vetor[i]);
        }
        return texto.append(']').toString();
    }
}
