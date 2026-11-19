package br.com.fiap.trabalhojusto.service;

import br.com.fiap.trabalhojusto.model.TipoContratacao;
import br.com.fiap.trabalhojusto.regras.MotorRegras;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Spring AI (uso 2): transforma o resultado técnico do motor de regras em uma explicação em linguagem simples.
 * A IA NÃO calcula nem decide nada: recebe os achados já calculados e só os reescreve.
 * Dados pessoais (nome, e-mail, empregador) nunca são enviados ao modelo.
 * Se a IA estiver indisponível, a análise continua com um texto padrão gerado sem IA.
 */
@Service
public class ExplicacaoIaService {

    private static final Logger log = LoggerFactory.getLogger(ExplicacaoIaService.class);
    private static final Locale PT_BR = Locale.of("pt", "BR");

    private static final String SISTEMA = """
            Você explica, em português do Brasil e em linguagem simples, o resultado de uma análise de condições
            de trabalho para uma pessoa sem conhecimento jurídico.
            Regras obrigatórias:
            - Use SOMENTE os fatos e valores fornecidos. Não invente números, datas, leis ou artigos.
            - Sempre fale em "possível irregularidade" e "valor de referência". Nunca afirme que a lei foi violada
              nem prometa que a pessoa tem direito a receber algo.
            - Organize em no máximo 3 parágrafos curtos, sem listas longas e sem juridiquês.
            - Termine sugerindo procurar o sindicato da categoria, a Defensoria Pública ou um advogado trabalhista.
            """;

    private final ChatClient chatClient;

    public ExplicacaoIaService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String explicar(TipoContratacao tipo, MotorRegras.Resultado resultado) {
        if (resultado.achados().isEmpty()) {
            return "Com os dados informados neste período, não foram encontrados sinais de irregularidade. "
                    + "Isso não garante que tudo esteja correto: confira se todas as jornadas e pagamentos foram registrados.";
        }
        try {
            String fatos = montarFatos(tipo, resultado);
            String texto = chatClient.prompt()
                    .system(SISTEMA)
                    .user(u -> u.text("{fatos}").param("fatos", fatos))
                    .call()
                    .content();
            if (texto != null && !texto.isBlank()) {
                return texto.trim();
            }
        } catch (Exception e) {
            log.warn("IA indisponível para gerar explicação; usando texto padrão: {}", e.getMessage());
        }
        return textoPadrao(resultado);
    }

    private String montarFatos(TipoContratacao tipo, MotorRegras.Resultado resultado) {
        StringBuilder sb = new StringBuilder();
        sb.append("Tipo de contratação: ").append(tipo).append('\n');
        sb.append("Diferença total de referência: ").append(moeda(resultado.diferencaTotalEstimada())).append('\n');
        sb.append("Sinais identificados pelo sistema:\n");
        resultado.achados().forEach(a -> sb.append("- ").append(a.descricao()).append('\n'));
        return sb.toString();
    }

    private String textoPadrao(MotorRegras.Resultado resultado) {
        return "Foram encontrados " + resultado.achados().size() + " sinais de possível irregularidade neste período. "
                + "A diferença total de referência é de " + moeda(resultado.diferencaTotalEstimada()) + ". "
                + "Veja os detalhes abaixo e procure o sindicato da categoria, a Defensoria Pública ou um advogado trabalhista.";
    }

    private String moeda(BigDecimal valor) {
        return String.format(PT_BR, "R$ %,.2f", valor);
    }
}
