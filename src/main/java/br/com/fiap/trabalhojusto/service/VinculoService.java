package br.com.fiap.trabalhojusto.service;

import br.com.fiap.trabalhojusto.dto.PaginaResponse;
import br.com.fiap.trabalhojusto.dto.VinculoDto;
import br.com.fiap.trabalhojusto.exception.RecursoNaoEncontradoException;
import br.com.fiap.trabalhojusto.model.Usuario;
import br.com.fiap.trabalhojusto.model.VinculoTrabalhista;
import br.com.fiap.trabalhojusto.repository.UsuarioRepository;
import br.com.fiap.trabalhojusto.repository.VinculoRepository;
import br.com.fiap.trabalhojusto.util.Paginacao;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VinculoService {

    private final VinculoRepository vinculos;
    private final UsuarioRepository usuarios;

    public VinculoService(VinculoRepository vinculos, UsuarioRepository usuarios) {
        this.vinculos = vinculos;
        this.usuarios = usuarios;
    }

    @Transactional
    public VinculoDto.Response criar(String email, VinculoDto.Request req) {
        Usuario usuario = usuarios.findByEmail(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));
        VinculoTrabalhista v = new VinculoTrabalhista();
        v.setUsuario(usuario);
        aplicar(v, req);
        return VinculoDto.Response.de(vinculos.save(v));
    }

    @Transactional(readOnly = true)
    public PaginaResponse<VinculoDto.Response> listar(String email, int pagina, int tamanho) {
        Pageable pageable = Paginacao.de(pagina, tamanho, Sort.by(Sort.Direction.DESC, "id"));
        return PaginaResponse.de(vinculos.findByUsuario_Email(email, pageable), VinculoDto.Response::de);
    }

    @Transactional(readOnly = true)
    public VinculoDto.Response buscar(String email, Long id) {
        return VinculoDto.Response.de(obter(email, id));
    }

    @Transactional
    public VinculoDto.Response atualizar(String email, Long id, VinculoDto.Request req) {
        VinculoTrabalhista v = obter(email, id);
        aplicar(v, req);
        return VinculoDto.Response.de(vinculos.save(v));
    }

    @Transactional
    public void excluir(String email, Long id) {
        vinculos.delete(obter(email, id));
    }

    /**
     * Retorna o vínculo somente se pertencer ao usuário autenticado. Vínculo de outra pessoa
     * responde 404 (e não 403) para não revelar que o id existe.
     */
    @Transactional(readOnly = true)
    public VinculoTrabalhista obter(String email, Long id) {
        return vinculos.findByIdAndUsuario_Email(id, email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vínculo não encontrado"));
    }

    private void aplicar(VinculoTrabalhista v, VinculoDto.Request req) {
        v.setEmpregador(req.empregador().trim());
        v.setTipoContratacao(req.tipoContratacao());
        v.setSalarioMensal(req.salarioMensal());
        v.setJornadaSemanalHoras(req.jornadaSemanalHoras());
        v.setDataInicio(req.dataInicio());
        if (req.ativo() != null) {
            v.setAtivo(req.ativo());
        }
    }
}
