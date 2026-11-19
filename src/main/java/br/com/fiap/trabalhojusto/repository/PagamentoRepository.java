package br.com.fiap.trabalhojusto.repository;

import br.com.fiap.trabalhojusto.model.Pagamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    Page<Pagamento> findByVinculoId(Long vinculoId, Pageable pageable);

    Optional<Pagamento> findByIdAndVinculoId(Long id, Long vinculoId);

    boolean existsByVinculoIdAndCompetencia(Long vinculoId, LocalDate competencia);

    boolean existsByVinculoIdAndCompetenciaAndIdNot(Long vinculoId, LocalDate competencia, Long id);

    List<Pagamento> findByVinculoIdAndCompetenciaBetween(Long vinculoId, LocalDate inicio, LocalDate fim);
}
