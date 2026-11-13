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

/** Métrica de avaliação de um modelo de aprendizado de máquina, gravada a cada treino. */
@Entity
@Table(name = "metrica_modelo_ia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricaModeloIa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "modelo", nullable = false, length = 40)
    private String modelo;

    @Column(name = "metrica", nullable = false, length = 40)
    private String metrica;

    @Column(name = "valor", nullable = false)
    private Double valor;

    @Column(name = "amostras", nullable = false)
    private Integer amostras;

    @Column(name = "calculada_em", nullable = false)
    private LocalDateTime calculadaEm;
}
