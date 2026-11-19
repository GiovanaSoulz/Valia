package br.com.fiap.trabalhojusto.repository;

import br.com.fiap.trabalhojusto.model.VinculoTrabalhista;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface VinculoRepository extends JpaRepository<VinculoTrabalhista, Long> {

    Page<VinculoTrabalhista> findByUsuario_Email(String email, Pageable pageable);

    Optional<VinculoTrabalhista> findByIdAndUsuario_Email(Long id, String email);

    /** Agregado para o painel do gestor: [TipoContratacao, quantidade]. */
    @Query("select v.tipoContratacao, count(v) from VinculoTrabalhista v group by v.tipoContratacao")
    List<Object[]> contarPorTipoContratacao();
}
