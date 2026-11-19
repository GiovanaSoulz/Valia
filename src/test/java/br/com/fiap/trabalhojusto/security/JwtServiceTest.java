package br.com.fiap.trabalhojusto.security;

import br.com.fiap.trabalhojusto.model.Papel;
import br.com.fiap.trabalhojusto.model.Usuario;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres-123456";

    private Usuario usuario() {
        Usuario u = new Usuario();
        u.setEmail("ana@exemplo.com");
        u.setPapel(Papel.GESTOR);
        return u;
    }

    @Test
    void tokenValidoPreservaEmailEPapel() {
        JwtService service = new JwtService(SEGREDO, 60);

        var dados = service.validar(service.gerar(usuario()));

        assertThat(dados).isPresent();
        assertThat(dados.get().email()).isEqualTo("ana@exemplo.com");
        assertThat(dados.get().papel()).isEqualTo(Papel.GESTOR);
    }

    @Test
    void tokenExpiradoEhRejeitado() {
        JwtService service = new JwtService(SEGREDO, -10);

        assertThat(service.validar(service.gerar(usuario()))).isEmpty();
    }

    @Test
    void tokenAdulteradoEhRejeitado() {
        JwtService service = new JwtService(SEGREDO, 60);
        String token = service.gerar(usuario());

        assertThat(service.validar(token + "x")).isEmpty();
        assertThat(service.validar("lixo.qualquer.coisa")).isEmpty();
    }

    @Test
    void tokenAssinadoComOutraChaveEhRejeitado() {
        JwtService emissor = new JwtService(SEGREDO, 60);
        JwtService outro = new JwtService("outro-segredo-de-teste-com-mais-de-32-caracteres", 60);

        assertThat(outro.validar(emissor.gerar(usuario()))).isEmpty();
    }

    @Test
    void segredoCurtoEhRecusado() {
        assertThatThrownBy(() -> new JwtService("curto", 60)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void semSegredoUsaChaveAleatoriaFuncional() {
        JwtService service = new JwtService("", 60);

        assertThat(service.validar(service.gerar(usuario()))).isPresent();
    }
}
