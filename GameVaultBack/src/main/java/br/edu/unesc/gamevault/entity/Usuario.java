package br.edu.unesc.gamevault.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.edu.unesc.gamevault.entity.enums.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "email", nullable = false, length = 150, unique = true)
    private String email;

    @Column(name = "senha", nullable = false, length = 100)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "saldo", nullable = false, precision = 10, scale = 2)
    private BigDecimal saldo;

    @Column(name = "data_cadastro", nullable = false)
    private LocalDateTime dataCadastro;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo;

    @PrePersist
    private void aoPersistir() {
        if (dataCadastro == null) {
            dataCadastro = LocalDateTime.now();
        }
        if (saldo == null) {
            saldo = BigDecimal.ZERO;
        }
        if (ativo == null) {
            ativo = Boolean.TRUE;
        }
    }
}
