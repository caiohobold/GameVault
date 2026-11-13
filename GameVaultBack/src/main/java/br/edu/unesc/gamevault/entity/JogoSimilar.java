package br.edu.unesc.gamevault.entity;

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

/** Par de jogos parecidos, calculado no treino da recomendação. */
@Entity
@Table(name = "jogo_similar")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JogoSimilar {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "jogo_id", nullable = false)
    private Long jogoId;

    @Column(name = "similar_id", nullable = false)
    private Long similarId;

    @Column(name = "pontuacao", nullable = false)
    private Double pontuacao;
}
