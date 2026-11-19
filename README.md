# Trabalho Justo — backend (Global Solution · Java Advanced · CitizenTech)

API REST em **Java 21 + Spring Boot** que ajuda trabalhadores (domésticos, informais e CLT) a identificar
**possíveis** irregularidades nas próprias condições de trabalho e a estimar **valores de referência**
que deixaram de receber. A mesma API é consumida pelo app Mobile.

> Os resultados são sinais e valores de referência, **não** parecer jurídico. Toda resposta de relatório traz esse aviso.

## Como funciona

1. A pessoa descreve a rotina em texto livre → **Spring AI** extrai os campos (`POST /ia/interpretar-relato`) e o app pré-preenche vínculo e jornada.
2. Ela cadastra o **vínculo**, as **jornadas** e os **pagamentos** recebidos.
3. Solicita uma **análise** de um período (`POST /vinculos/{id}/analises`). A API responde `202` e publica um evento no **RabbitMQ**.
4. O consumidor roda o **motor de regras determinístico** (horas extras, intervalos, descanso entre jornadas, salário mínimo proporcional, diferença de pagamento) e usa a IA só para **explicar o resultado em linguagem simples**.
5. O app consulta `GET /analises/{id}` até o status ser `CONCLUIDO`.

A IA **nunca calcula nem decide**: o cálculo jurídico/financeiro é código determinístico, versionado e testado.

## Opção escolhida no requisito 5: **A) Mensageria com RabbitMQ**

Evento `análise solicitada` publicado por `AnaliseService` e consumido de forma assíncrona por `AnaliseSolicitadaListener`
(fila `trabalhojusto.analise.solicitada`). A mensagem contém só o id do relatório; nenhum dado pessoal trafega pela fila.

## Tecnologias

Spring Boot, Spring Data JPA (PostgreSQL), Spring Security + JWT (jjwt), Spring AI (OpenAI), Spring AMQP (RabbitMQ),
Bean Validation, springdoc-openapi (Swagger), Lombok, JUnit 5, Docker, GitHub Actions.

## Como rodar

**Pré-requisitos:** JDK 21, Docker e (opcional) uma chave da OpenAI.

```bash
# 1) infraestrutura (PostgreSQL + RabbitMQ)
docker compose up -d

# 2) variáveis de ambiente (veja .env.example)
export OPENAI_API_KEY=sua-chave        # opcional: sem ela a IA usa texto padrão / responde 503
export JWT_SECRET=um-segredo-aleatorio-com-no-minimo-32-caracteres

# 3) subir a API
./mvnw spring-boot:run
```

- **IntelliJ:** abra o `pom.xml` como projeto, ative *Enable annotation processing* (Lombok), rode `TrabalhojustoApplication`
  e informe as variáveis em *Run > Edit Configurations > Environment variables*.
- **Tudo em container:** `docker compose --profile app up -d --build`.
- **Swagger:** http://localhost:8080/swagger-ui.html · **RabbitMQ:** http://localhost:15672 (guest/guest)
- **Testes:** `./mvnw verify` (usam H2 em memória; não precisam de Docker nem de chave de IA).

Sem `JWT_SECRET` a aplicação gera uma chave aleatória a cada execução (os tokens deixam de valer ao reiniciar).
Nenhum segredo fica no código-fonte.

## Usuários de teste (criados na inicialização; desative com `SEED_ENABLED=false`)

| Papel | E-mail | Senha |
|---|---|---|
| TRABALHADOR | trabalhador@trabalhojusto.com | Trabalhador@123 |
| GESTOR | gestor@trabalhojusto.com | Gestor@123 |

O trabalhador de teste já vem com um vínculo doméstico, jornadas do mês anterior (9h/dia, 30 min de intervalo) e um pagamento só do salário-base, para demonstrar irregularidades no vídeo e na apresentação.

## Perfis e autorização

| Papel | Pode |
|---|---|
| `TRABALHADOR` | CRUD dos **próprios** vínculos, jornadas e pagamentos; solicitar e ver os próprios relatórios; usar a IA |
| `GESTOR` | somente `GET /gestor/estatisticas` (dados **agregados e anonimizados**) |

Dados de outra pessoa respondem `404` (não `403`), para não revelar a existência do recurso.
O cadastro público (`/auth/registro`) cria sempre `TRABALHADOR`.

## Principais endpoints

| Método | Rota | Descrição |
|---|---|---|
| POST | `/auth/registro` | cadastra trabalhador |
| POST | `/auth/login` | devolve o JWT (`Authorization: Bearer <token>`) |
| POST / GET | `/vinculos` | cria / lista (paginado) |
| GET / PUT / DELETE | `/vinculos/{id}` | consulta / atualiza / exclui |
| POST / GET | `/vinculos/{id}/jornadas` | registra / lista jornadas (uma por data) |
| GET / PUT / DELETE | `/vinculos/{id}/jornadas/{jid}` | |
| POST / GET | `/vinculos/{id}/pagamentos` | registra / lista pagamentos (um por competência) |
| GET / PUT / DELETE | `/vinculos/{id}/pagamentos/{pid}` | |
| POST | `/ia/interpretar-relato` | **Spring AI**: texto livre → campos estruturados |
| POST | `/vinculos/{id}/analises` | solicita análise (`202`, assíncrona via RabbitMQ) |
| GET | `/vinculos/{id}/analises` | lista relatórios do vínculo (paginado) |
| GET | `/analises/{id}` | relatório completo + explicação em linguagem simples |
| GET | `/gestor/estatisticas` | painel agregado (somente GESTOR) |

Listas aceitam `?pagina=0&tamanho=10` e retornam `{conteudo, pagina, tamanho, totalElementos, totalPaginas}`.
Erros seguem sempre o formato `{timestamp, status, erro, mensagem, caminho, detalhes[]}`.

### Exemplo rápido

```bash
TOKEN=$(curl -s localhost:8080/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"trabalhador@trabalhojusto.com","senha":"Trabalhador@123"}' | jq -r .token)

curl -s localhost:8080/vinculos -H "Authorization: Bearer $TOKEN"

curl -s -X POST localhost:8080/vinculos/1/analises -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"periodoInicio":"2026-10-01","periodoFim":"2026-10-31"}'

curl -s localhost:8080/analises/1 -H "Authorization: Bearer $TOKEN"
```

(Ajuste o período para o mês anterior à data em que você subir a aplicação: é o mês dos dados de demonstração.)

## Modelagem

`Usuario` 1—N `VinculoTrabalhista` 1—N `RegistroJornada` · `Pagamento` · `RelatorioAnalise` 1—N `IrregularidadeDetectada`.
Regras de integridade: uma jornada por data e um pagamento por competência em cada vínculo; exclusão de vínculo apaga seus dados em cascata.

## Motor de regras (`regras/`)

- `CatalogoRegras` guarda **versões** dos parâmetros legais por data de vigência (salário mínimo, 8h/dia, 44h/semana, adicional de 50%, intervalo de 1h, descanso de 11h). Cada relatório registra a `versaoRegras` usada, então cálculos antigos são reproduzíveis.
- `MotorRegras` é Java puro (sem Spring), determinístico e coberto por testes (`MotorRegrasTest`) com valores conferidos manualmente (`BigDecimal`, `HALF_UP`).
- Base de referência: CF/88 art. 7º; CLT arts. 58, 59, 66 e 71; LC 150/2015 (doméstico).
- **Atenção:** os parâmetros (principalmente o salário mínimo de cada ano) e as regras devem ser **revisados por profissional jurídico** antes de qualquer uso real.

## Compliance, LGPD e segurança

- Dados de jornada e remuneração são pessoais: o gestor só vê agregados anonimizados; cada trabalhador só acessa os próprios dados.
- **Nada de dado pessoal é enviado ao modelo de IA** (sem nome, e-mail ou empregador): a explicação recebe apenas os achados já calculados; a interpretação de relato recebe só o texto digitado pela pessoa, que o escreve ciente do envio.
- Linguagem sempre cautelosa ("possível", "valor de referência") e aviso de que não substitui orientação profissional.
- Senhas com BCrypt, JWT assinado (HS256) com expiração, sessão stateless, chave e credenciais por variável de ambiente.
- Prompt do relato trata o texto do usuário como dado (mitigação básica de prompt injection) e a resposta do modelo é validada antes de sair da API.
- Melhorias futuras: consentimento explícito versionado, política de retenção/exclusão de dados, k-anonimato mínimo no painel do gestor.

## DevOps

`Dockerfile` multi-stage, `docker-compose.yml` (PostgreSQL, RabbitMQ e a API), GitHub Actions (`.github/workflows/ci.yml`) rodando `mvn verify`.

## Link do deploy

_Não há deploy_ (preencha aqui se publicarem).

## Declaração de uso de IA

> **Revisem e ajustem este texto para refletir o que o grupo realmente fez.**
>
> Este projeto usa IA generativa de duas formas: (1) **no produto** — Spring AI com modelo da OpenAI para interpretar relatos em texto livre e para explicar resultados em linguagem simples (a IA não faz cálculos nem decisões jurídicas); (2) **no desenvolvimento** — parte do código, dos testes e da documentação foi gerada com apoio do assistente Claude (Anthropic) e revisada, executada e adaptada pelo grupo, que se responsabiliza pelo conteúdo entregue.

## Vídeo pitch

_Cole aqui o link público (YouTube não listado ou Drive aberto)._
