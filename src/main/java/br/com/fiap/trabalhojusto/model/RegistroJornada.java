package br.com.fiap.trabalhojusto.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "registros_jornada",
        uniqueConstraints = @UniqueConstraint(columnNames = {"vinculo_id", "data_jornada"}))
@Getter
@Setter
@NoArgsConstructor
public class RegistroJornada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vinculo_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private VinculoTrabalhista vinculo;

    @Column(name = "data_jornada", nullable = false)
    private LocalDate data;

    @Column(name = "hora_entrada", nullable = false)
    private LocalTime horaEntrada;

    @Column(name = "hora_saida", nullable = false)
    private LocalTime horaSaida;

    @Column(name = "intervalo_minutos", nullable = false)
    private Integer intervaloMinutos;
}
