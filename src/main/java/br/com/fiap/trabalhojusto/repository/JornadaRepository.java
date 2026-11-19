package br.com.fiap.trabalhojusto.repository;

import br.com.fiap.trabalhojusto.model.RegistroJornada;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface JornadaRepository extends JpaRepository<RegistroJornada, Long> {

    Page<RegistroJornada> findByVinculoId(Long vinculoId, Pageable pageable);

    Optional<RegistroJornada> findByIdAndVinculoId(Long id, Long vinculoId);

    boolean existsByVinculoIdAndData(Long vinculoId, LocalDate data);

    boolean existsByVinculoIdAndDataAndIdNot(Long vinculoId, LocalDate data, Long id);

    List<RegistroJornada> findByVinculoIdAndDataBetweenOrderByDataAsc(Long vinculoId, LocalDate inicio, LocalDate fim);
}
