package br.com.fiap.trabalhojusto.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "vinculos_trabalhistas")
@Getter
@Setter
@NoArgsConstructor
public class VinculoTrabalhista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 120)
    private String empregador;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_contratacao", nullable = false, length = 20)
    private TipoContratacao tipoContratacao;

    @Column(name = "salario_mensal", nullable = false, precision = 12, scale = 2)
    private BigDecimal salarioMensal;

    @Column(name = "jornada_semanal_horas", nullable = false)
    private Integer jornadaSemanalHoras;

    @Column(name = "data_inicio", nullable = false)
    private LocalDate dataInicio;

    @Column(nullable = false)
    private boolean ativo = true;
}
