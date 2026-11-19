package br.com.fiap.trabalhojusto.config;

import br.com.fiap.trabalhojusto.model.Pagamento;
import br.com.fiap.trabalhojusto.model.Papel;
import br.com.fiap.trabalhojusto.model.RegistroJornada;
import br.com.fiap.trabalhojusto.model.TipoContratacao;
import br.com.fiap.trabalhojusto.model.Usuario;
import br.com.fiap.trabalhojusto.model.VinculoTrabalhista;
import br.com.fiap.trabalhojusto.repository.JornadaRepository;
import br.com.fiap.trabalhojusto.repository.PagamentoRepository;
import br.com.fiap.trabalhojusto.repository.UsuarioRepository;
import br.com.fiap.trabalhojusto.repository.VinculoRepository;
import br.com.fiap.trabalhojusto.util.Tempo;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;

/**
 * Cria usuários de teste (gestor e trabalhador) e dados de demonstração para o app e para a apresentação.
 * Desative em produção com SEED_ENABLED=false.
 */
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true")
public class DataInitializer implements CommandLineRunner {

    public static final String EMAIL_GESTOR = "gestor@trabalhojusto.com";
    public static final String EMAIL_TRABALHADOR = "trabalhador@trabalhojusto.com";

    private final UsuarioRepository usuarios;
    private final VinculoRepository vinculos;
    private final JornadaRepository jornadas;
    private final PagamentoRepository pagamentos;
    private final PasswordEncoder encoder;

    public DataInitializer(UsuarioRepository usuarios, VinculoRepository vinculos, JornadaRepository jornadas,
                           PagamentoRepository pagamentos, PasswordEncoder encoder) {
        this.usuarios = usuarios;
        this.vinculos = vinculos;
        this.jornadas = jornadas;
        this.pagamentos = pagamentos;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        criarUsuario("Gestor Demo", EMAIL_GESTOR, "Gestor@123", Papel.GESTOR);
        Usuario trabalhador = criarUsuario("Trabalhadora Demo", EMAIL_TRABALHADOR, "Trabalhador@123", Papel.TRABALHADOR);
        if (vinculos.findByUsuario_Email(trabalhador.getEmail(), PageRequest.of(0, 1)).isEmpty()) {
            criarDadosDemo(trabalhador);
        }
    }

    private Usuario criarUsuario(String nome, String email, String senha, Papel papel) {
        return usuarios.findByEmail(email).orElseGet(() -> {
            Usuario u = new Usuario();
            u.setNome(nome);
            u.setEmail(email);
            u.setSenhaHash(encoder.encode(senha));
            u.setPapel(papel);
            return usuarios.save(u);
        });
    }

    /** Trabalhadora doméstica, jornada de 9h/dia com 30 min de intervalo e pagamento só do salário-base. */
    private void criarDadosDemo(Usuario usuario) {
        VinculoTrabalhista v = new VinculoTrabalhista();
        v.setUsuario(usuario);
        v.setEmpregador("Família Exemplo");
        v.setTipoContratacao(TipoContratacao.DOMESTICO);
        v.setSalarioMensal(new BigDecimal("1800.00"));
        v.setJornadaSemanalHoras(44);
        v.setDataInicio(LocalDate.of(2025, 1, 10));
        v = vinculos.save(v);

        YearMonth mesAnterior = YearMonth.now(Tempo.ZONA).minusMonths(1);
        for (int dia = 1; dia <= mesAnterior.lengthOfMonth(); dia++) {
            LocalDate data = mesAnterior.atDay(dia);
            if (data.getDayOfWeek() == DayOfWeek.SATURDAY || data.getDayOfWeek() == DayOfWeek.SUNDAY) {
                continue;
            }
            RegistroJornada j = new RegistroJornada();
            j.setVinculo(v);
            j.setData(data);
            j.setHoraEntrada(LocalTime.of(8, 0));
            j.setHoraSaida(LocalTime.of(17, 30));
            j.setIntervaloMinutos(30);
            jornadas.save(j);
        }

        Pagamento p = new Pagamento();
        p.setVinculo(v);
        p.setCompetencia(mesAnterior.atDay(1));
        p.setValorPago(new BigDecimal("1800.00"));
        p.setDataPagamento(mesAnterior.plusMonths(1).atDay(5));
        pagamentos.save(p);
    }
}
