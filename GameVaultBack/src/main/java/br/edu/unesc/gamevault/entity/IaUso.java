package br.edu.unesc.gamevault.entity;

import java.time.LocalDateTime;

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

/** Uma chamada à API do Claude: tokens, duração e se deu certo. */
@Entity
@Table(name = "ia_uso")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IaUso {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "modulo", nullable = false, length = 40)
    private String modulo;

    @Column(name = "modelo", nullable = false, length = 60)
    private String modelo;

    @Column(name = "usuario_id")
    private Long usuarioId;

    @Column(name = "tokens_entrada", nullable = false)
    private Long tokensEntrada;

    @Column(name = "tokens_saida", nullable = false)
    private Long tokensSaida;

    @Column(name = "tokens_cache_leitura", nullable = false)
    private Long tokensCacheLeitura;

    @Column(name = "duracao_ms", nullable = false)
    private Long duracaoMs;

    @Column(name = "sucesso", nullable = false)
    private Boolean sucesso;

    @Column(name = "erro", length = 500)
    private String erro;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;
}
