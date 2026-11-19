package br.com.fiap.trabalhojusto.service;

import br.com.fiap.trabalhojusto.dto.GestorDto;
import br.com.fiap.trabalhojusto.model.Papel;
import br.com.fiap.trabalhojusto.model.StatusAnalise;
import br.com.fiap.trabalhojusto.model.TipoContratacao;
import br.com.fiap.trabalhojusto.model.TipoIrregularidade;
import br.com.fiap.trabalhojusto.repository.IrregularidadeRepository;
import br.com.fiap.trabalhojusto.repository.RelatorioAnaliseRepository;
import br.com.fiap.trabalhojusto.repository.UsuarioRepository;
import br.com.fiap.trabalhojusto.repository.VinculoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class GestorService {

    private final UsuarioRepository usuarios;
    private final VinculoRepository vinculos;
    private final RelatorioAnaliseRepository relatorios;
    private final IrregularidadeRepository irregularidades;

    public GestorService(UsuarioRepository usuarios, VinculoRepository vinculos,
                         RelatorioAnaliseRepository relatorios, IrregularidadeRepository irregularidades) {
        this.usuarios = usuarios;
        this.vinculos = vinculos;
        this.relatorios = relatorios;
        this.irregularidades = irregularidades;
    }

    @Transactional(readOnly = true)
    public GestorDto.Estatisticas estatisticas() {
        List<GestorDto.PorContratacao> porContratacao = vinculos.contarPorTipoContratacao().stream()
                .map(l -> new GestorDto.PorContratacao((TipoContratacao) l[0], ((Number) l[1]).longValue()))
                .toList();
        List<GestorDto.PorIrregularidade> porTipo = irregularidades.resumoPorTipo().stream()
                .map(l -> new GestorDto.PorIrregularidade((TipoIrregularidade) l[0], ((Number) l[1]).longValue(),
                        l[2] == null ? BigDecimal.ZERO : (BigDecimal) l[2]))
                .toList();
        BigDecimal total = porTipo.stream().map(GestorDto.PorIrregularidade::valorEstimadoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new GestorDto.Estatisticas(
                usuarios.countByPapel(Papel.TRABALHADOR),
                vinculos.count(),
                relatorios.countByStatus(StatusAnalise.CONCLUIDO),
                total,
                porContratacao,
                porTipo);
    }
}
