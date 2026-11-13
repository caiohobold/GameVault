package br.edu.unesc.gamevault.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Resumo das avaliações de um jogo, gerado pelo Claude. Um por jogo. */
@Entity
@Table(name = "resumo_avaliacao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumoAvaliacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "jogo_id", nullable = false, unique = true)
    private Long jogoId;

    @Column(name = "resumo", nullable = false, columnDefinition = "text")
    private String resumo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pontos_positivos", nullable = false)
    private List<String> pontosPositivos;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "pontos_negativos", nullable = false)
    private List<String> pontosNegativos;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "aspectos", nullable = false)
    private List<SentimentoAspecto> aspectos;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "avaliacoes_divergentes", nullable = false)
    private List<Long> avaliacoesDivergentes;

    @Column(name = "total_avaliacoes_base", nullable = false)
    private Integer totalAvaliacoesBase;

    @Column(name = "gerado_em", nullable = false)
    private LocalDateTime geradoEm;
}
