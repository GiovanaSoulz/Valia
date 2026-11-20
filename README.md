# VALIA — backend (Global Solution · Java Advanced · CitizenTech)

API REST em **Java 21 + Spring Boot** que ajuda trabalhadores (domésticos, informais e CLT) a identificar
**possíveis** irregularidades nas próprias condições de trabalho e a estimar **valores de referência**
que deixaram de receber.

> Os resultados são sinais e valores de referência, **não** parecer jurídico. Toda resposta de relatório traz esse aviso.

## Sumário

1. [Como funciona](#como-funciona)
2. [Opção escolhida no requisito 5](#opção-escolhida-no-requisito-5-a-mensageria-com-rabbitmq)
3. [Tecnologias](#tecnologias)
4. [Tutorial: do zero até a API rodando](#tutorial-do-zero-até-a-api-rodando)
5. [Tutorial: IA com Ollama](#tutorial-ia-com-ollama)
6. [Tutorial: usando o Swagger](#tutorial-usando-o-swagger)
7. [Usuários de teste](#usuários-de-teste)
8. [Perfis e autorização](#perfis-e-autorização)
9. [Principais endpoints](#principais-endpoints)
10. [Problemas comuns](#problemas-comuns)
11. [Modelagem, motor de regras, compliance e DevOps](#modelagem)

## Como funciona

1. A pessoa descreve a rotina em texto livre → **Spring AI** (modelo local via Ollama) extrai os campos (`POST /ia/interpretar-relato`) e o app pré-preenche vínculo e jornada.
2. Ela cadastra o **vínculo**, as **jornadas** e os **pagamentos** recebidos.
3. Solicita uma **análise** de um período (`POST /vinculos/{id}/analises`). A API responde `202` e publica um evento no **RabbitMQ**.
4. O consumidor roda o **motor de regras determinístico** (horas extras, intervalos, descanso entre jornadas, salário mínimo proporcional, diferença de pagamento) e usa a IA só para **explicar o resultado em linguagem simples**.
5. O app consulta `GET /analises/{id}` até o status ser `CONCLUIDO`.

A IA **nunca calcula nem decide**: o cálculo jurídico/financeiro é código determinístico, versionado e testado.

## Opção escolhida no requisito 5: A) Mensageria com RabbitMQ

Evento `análise solicitada` publicado por `AnaliseService` e consumido de forma assíncrona por `AnaliseSolicitadaListener`
(fila `valia.analise.solicitada`). A mensagem contém só o id do relatório; nenhum dado pessoal trafega pela fila.

## Tecnologias

Java 21, Spring Boot, Spring Data JPA (PostgreSQL), Spring Security + JWT (jjwt), **Spring AI (Ollama, modelo local)**,
Spring AMQP (RabbitMQ), Bean Validation, springdoc-openapi (Swagger), Lombok, JUnit 5, Docker, GitHub Actions.

---

## Tutorial: do zero até a API rodando

### O que instalar antes

| Programa | Para quê | Onde baixar |
|---|---|---|
| **JDK 21** | rodar o Java | https://adoptium.net |
| **Docker Desktop** | PostgreSQL e RabbitMQ | https://www.docker.com/products/docker-desktop |
| **Ollama** | IA local (veja o tutorial abaixo) | https://ollama.com/download |
| **IntelliJ IDEA** (opcional) | abrir e rodar o projeto | https://www.jetbrains.com/idea |

Confira no terminal se o Java e o Docker estão instalados:

```bash
java -version      # deve mostrar a versão 21
docker --version
```

### Passo 1 — Subir o banco e o RabbitMQ

Abra o **Docker Desktop** (ele precisa estar aberto e rodando) e, no terminal, dentro da pasta do projeto (a que tem o `pom.xml`):

```bash
docker compose up -d
docker compose ps
```

Os serviços `postgres` e `rabbitmq` devem aparecer como `running` (ou `healthy`).

- PostgreSQL: porta `5432` (banco, usuário e senha: `valia`)
- RabbitMQ: porta `5672`; painel em http://localhost:15672 (usuário `guest`, senha `guest`)

> Se aparecer `port is already allocated`, outro container ou programa está usando a porta 5432.
> Veja a seção [Problemas comuns](#problemas-comuns).

### Passo 2 — Subir a IA (Ollama)

Siga o [Tutorial: IA com Ollama](#tutorial-ia-com-ollama) abaixo. Resumo: instalar o Ollama e rodar `ollama pull llama3.2`.

### Passo 3 — Definir o segredo do JWT (recomendado)

O JWT é a "chave" que assina os tokens de login. Defina um segredo com **no mínimo 32 caracteres**.

- **Windows (PowerShell):** `$env:JWT_SECRET="troque-por-um-segredo-aleatorio-com-32-ou-mais-caracteres"`
- **Linux/macOS:** `export JWT_SECRET=troque-por-um-segredo-aleatorio-com-32-ou-mais-caracteres`

Se você não definir, a API gera uma chave aleatória a cada execução (funciona, mas os tokens deixam de valer sempre que a API reinicia).

### Passo 4 — Rodar a API

**Pelo terminal** (na pasta do projeto):

```bash
./mvnw spring-boot:run          # Linux/macOS
.\mvnw.cmd spring-boot:run      # Windows (PowerShell)
```

**Pelo IntelliJ:**

1. **File > Open** e escolha a pasta do projeto (a que tem o `pom.xml`).
2. Se pedir, clique em **Trust Project** e aguarde o Maven baixar as dependências.
3. Ative o Lombok: **Settings > Build, Execution, Deployment > Compiler > Annotation Processors > Enable annotation processing**.
4. Abra `src/main/java/br/com/fiap/valia/ValiaApplication.java` e clique no triângulo verde ao lado de `main` > **Run**.
5. Para passar variáveis (como `JWT_SECRET`): **Run > Edit Configurations > Environment variables**.

### Como saber que deu certo

No console deve aparecer algo como `Started ValiaApplication in ... seconds`. Abra no navegador:

**http://localhost:8080/swagger-ui.html**

### Tudo em container (opcional)

```bash
docker compose --profile app up -d --build
```

### Rodar os testes

```bash
./mvnw verify
```

Os testes usam banco em memória (H2); não precisam de Docker nem do Ollama.

---

## Tutorial: IA com Ollama

O Ollama roda o modelo de IA **no seu próprio computador**. Não precisa de chave, conta nem internet depois de baixar o modelo.

### 1. Instalar o Ollama

1. Baixe em https://ollama.com/download (Windows, macOS ou Linux) e instale.
2. No Windows, depois de instalado, o Ollama fica rodando em segundo plano (ícone da lhama perto do relógio).

Confirme no terminal:

```bash
ollama --version
```

### 2. Baixar o modelo

```bash
ollama pull llama3.2
```

O download é feito uma única vez (cerca de 2 GB). Para conferir que o modelo está disponível:

```bash
ollama list
```

A lista deve mostrar `llama3.2`.

### 3. Confirmar que o Ollama está rodando

Abra no navegador http://localhost:11434 — deve aparecer a mensagem **Ollama is running**.

Se não aparecer, abra o aplicativo **Ollama** pelo menu Iniciar, ou rode `ollama serve` em um terminal e deixe-o aberto.

### 4. Testar o modelo (opcional)

```bash
ollama run llama3.2
```

Escreva uma pergunta e confira que ele responde. Para sair, digite `/bye`.

### 5. Como a API usa o Ollama

As configurações ficam em `src/main/resources/application.properties` e podem ser trocadas por variáveis de ambiente:

| Variável | Padrão | Para quê |
|---|---|---|
| `OLLAMA_BASE_URL` | `http://localhost:11434` | endereço do Ollama |
| `OLLAMA_MODEL` | `llama3.2` | modelo usado |

Se quiser outro modelo, rode `ollama pull NOME-DO-MODELO` e defina `OLLAMA_MODEL=NOME-DO-MODELO`.

### 6. O que acontece se o Ollama estiver desligado

A API **sobe normalmente**. Só a parte de IA é afetada:

- `POST /ia/interpretar-relato` responde `503` (serviço indisponível);
- a explicação do relatório usa um texto padrão, sem IA.

Para a **demonstração e o vídeo**, deixe o Ollama rodando antes de iniciar a API. A primeira resposta da IA costuma demorar mais (o modelo é carregado na memória); as seguintes são mais rápidas.

### Se a API estiver em container (docker compose --profile app)

O container precisa alcançar o Ollama que está no seu computador. O `docker-compose.yml` já usa `http://host.docker.internal:11434`. No Windows e no macOS isso funciona direto.

---

## Tutorial: usando o Swagger

O Swagger é uma página que lista todos os endpoints e deixa você testá-los clicando, sem escrever código.

**Endereço:** http://localhost:8080/swagger-ui.html (a API precisa estar rodando).
A especificação em JSON fica em http://localhost:8080/v3/api-docs.

### Passo 1 — Fazer login e pegar o token

1. Na página, abra o grupo **Autenticação** e clique em `POST /auth/login`.
2. Clique em **Try it out**.
3. No campo *Request body*, cole:

```json
{
  "email": "trabalhador@valia.com",
  "senha": "Trabalhador@123"
}
```

4. Clique em **Execute**.
5. Na resposta (código `200`), copie **somente o valor** do campo `token` (o texto grande, sem as aspas).

### Passo 2 — Autorizar no Swagger

1. Clique no botão **Authorize** (cadeado, no topo da página à direita).
2. Cole o token no campo. **Não escreva a palavra `Bearer`**: só o token.
3. Clique em **Authorize** e depois em **Close**.

Agora o cadeado aparece fechado e todos os endpoints protegidos passam a funcionar.
O token expira em 1 hora; se começar a dar `401`, repita o Passo 1 e o Passo 2.

### Passo 3 — Testar os endpoints (roteiro sugerido)

Use sempre **Try it out** e depois **Execute**.

**a) Ver o vínculo de demonstração** — `GET /vinculos`
Deixe `pagina` = `0` e `tamanho` = `10`. Anote o `id` do vínculo (normalmente `1`).

**b) Cadastrar um novo vínculo** — `POST /vinculos`

```json
{
  "empregador": "Família Exemplo",
  "tipoContratacao": "DOMESTICO",
  "salarioMensal": 1800.00,
  "jornadaSemanalHoras": 44,
  "dataInicio": "2025-01-10"
}
```

Valores aceitos em `tipoContratacao`: `CLT`, `DOMESTICO`, `INFORMAL`, `TEMPORARIO`.

**c) Registrar uma jornada** — `POST /vinculos/{vinculoId}/jornadas` (informe o `id` do vínculo)

```json
{
  "data": "2026-11-10",
  "horaEntrada": "08:00",
  "horaSaida": "18:00",
  "intervaloMinutos": 30
}
```

Só pode haver uma jornada por data em cada vínculo.

**d) Registrar um pagamento** — `POST /vinculos/{vinculoId}/pagamentos`

```json
{
  "competencia": "2026-10-01",
  "valorPago": 1800.00,
  "dataPagamento": "2026-11-05"
}
```

Só pode haver um pagamento por mês de competência em cada vínculo.

**e) Usar a IA para interpretar um relato** — `POST /ia/interpretar-relato` (Ollama precisa estar rodando)

```json
{
  "texto": "Trabalho como diarista de segunda a sexta, entro às 7h e saio às 18h com meia hora de almoço, ganho 1800 por mês, sem carteira assinada."
}
```

A resposta traz os campos extraídos (tipo de contratação, salário, horários, intervalo) e a lista `camposNaoIdentificados`. A pessoa sempre revisa os dados antes de salvar.

**f) Pedir uma análise** — `POST /vinculos/{vinculoId}/analises`

Os dados de demonstração são do **mês anterior ao dia em que a API foi iniciada**. Se você subiu a API em novembro de 2026, use outubro:

```json
{
  "periodoInicio": "2026-10-01",
  "periodoFim": "2026-10-31"
}
```

A resposta é `202` (aceito): a análise roda de forma assíncrona pelo RabbitMQ. Anote o `id` do relatório.

**g) Ver o resultado** — `GET /analises/{id}`
Se o `status` ainda for `PENDENTE`, aguarde alguns segundos e execute de novo. Quando for `CONCLUIDO`, aparecem as irregularidades detectadas, o valor de referência e a **explicação em linguagem simples** (gerada pela IA).

**h) Painel do gestor** — `GET /gestor/estatisticas`
Exige o perfil **GESTOR**. Clique em **Authorize**, faça **Logout**, faça login com `gestor@valia.com` / `Gestor@123` (Passo 1), autorize com o novo token e execute.

### Entendendo as respostas

| Código | Significado |
|---|---|
| `200` / `201` | deu certo |
| `202` | análise aceita e em processamento |
| `400` | dados inválidos (a resposta diz qual campo) |
| `401` | sem token, token inválido ou expirado: refaça o login e o Authorize |
| `403` | seu perfil não tem acesso a esse endpoint |
| `404` | não encontrado (ou pertence a outra pessoa) |
| `503` | serviço indisponível: Ollama desligado ou fila do RabbitMQ fora do ar |

---

## Usuários de teste

Criados na inicialização (desative com `SEED_ENABLED=false`):

| Papel | E-mail | Senha |
|---|---|---|
| TRABALHADOR | trabalhador@valia.com | Trabalhador@123 |
| GESTOR | gestor@valia.com | Gestor@123 |

O trabalhador de teste já vem com um vínculo doméstico, jornadas do mês anterior (9h/dia, 30 min de intervalo) e um pagamento só do salário-base, para demonstrar irregularidades no vídeo e na apresentação.
Para criar outro trabalhador, use `POST /auth/registro` (nome, e-mail e senha de 8 a 72 caracteres).

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

### Exemplo rápido com curl

```bash
TOKEN=$(curl -s localhost:8080/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"trabalhador@valia.com","senha":"Trabalhador@123"}' | jq -r .token)

curl -s localhost:8080/vinculos -H "Authorization: Bearer $TOKEN"

curl -s -X POST localhost:8080/vinculos/1/analises -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' -d '{"periodoInicio":"2026-10-01","periodoFim":"2026-10-31"}'

curl -s localhost:8080/analises/1 -H "Authorization: Bearer $TOKEN"
```

(No Windows PowerShell use `curl.exe` ou, mais simples, o Swagger.)

## Problemas comuns

| Sintoma | Causa provável | O que fazer |
|---|---|---|
| `port is already allocated` ao subir o Postgres | outro container (por exemplo de um projeto antigo) ou programa usa a porta 5432 | `docker ps` para ver quem está usando; pare o antigo com `docker compose down` na pasta dele |
| `password authentication failed for user "valia"` | a API está falando com outro Postgres ou com um volume antigo | pare o outro container e rode `docker compose down -v` seguido de `docker compose up -d` aqui |
| API não sobe, erro de conexão com o RabbitMQ | RabbitMQ parado | `docker compose up -d` e confira com `docker compose ps` |
| `/ia/interpretar-relato` responde `503` | Ollama desligado ou modelo não baixado | abra o Ollama, rode `ollama list` e, se faltar, `ollama pull llama3.2` |
| `401` no Swagger | token expirado ou colado com `Bearer` | refaça o login e cole só o token no Authorize |
| Análise fica `PENDENTE` | consumidor ainda processando, ou RabbitMQ fora do ar | aguarde e consulte de novo; confira o painel http://localhost:15672 |
| A análise não encontra irregularidades | o período não tem os dados de demonstração | use o mês anterior ao dia em que a API foi iniciada |
| Erros de compilação com `get`/`set`/`builder` no IntelliJ | Lombok desativado | ative *Enable annotation processing* (Passo 4) |
| `JWT_SECRET deve ter pelo menos 32 caracteres` | segredo curto demais | use um segredo com 32 ou mais caracteres |

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
> Este projeto usa IA generativa de duas formas: (1) **no produto** — Spring AI com modelo local via Ollama (llama3.2) para interpretar relatos em texto livre e para explicar resultados em linguagem simples (a IA não faz cálculos nem decisões jurídicas); (2) **no desenvolvimento** — parte do código, dos testes e da documentação foi gerada com apoio do assistente Claude (Anthropic) e revisada, executada e adaptada pelo grupo, que se responsabiliza pelo conteúdo entregue.

## Vídeo pitch

_Cole aqui o link público (YouTube não listado ou Drive aberto)._
