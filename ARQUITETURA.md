# Arquitetura do Clipper

> Documento de ESTUDO. Além de descrever a estrutura, ele explica os PADRÕES por
> trás de cada decisão, usando o próprio código do Clipper como exemplo. Foca no
> PORQUÊ e na FORMA — o "o quê" está no código, que é a fonte da verdade.
>
> Como ler: passe pela visão geral e pelo fluxo primeiro (o mapa), depois estude
> a seção "Os padrões em ação" (a parte que ensina). No fim há uma lista de
> conceitos para aprofundar por conta própria.

## Visão geral

O Clipper é um helpdesk que recebe chamados de suporte e roda um diagnóstico
automatizado inicial ANTES do técnico humano. O usuário abre um ticket; o
Clipper lê o contexto, levanta a causa provável e os próximos passos; o técnico
recebe o caso com menos triagem manual.

Stack: backend Java 21 + Spring Boot; frontend React/Vite/TypeScript;
persistência JPA (H2 em arquivo hoje, Postgres no futuro).

## Princípios de arquitetura

- **Monólito modular com costuras.** Um deploy só agora, mas com fronteiras
  internas nítidas para poder fatiar em micro-serviço / multi-tenancy depois,
  sem bloquear o MVP.
- **Uma responsabilidade por classe.** Classes pequenas desde cedo; evitar a
  god-class. O `DiagnosticEngine` SÓ orquestra — não conhece detalhe de ticket
  nem de LLM.
- **Costuras (seams) nas fronteiras.** Onde dois mundos se encontram, um
  contrato no meio: `DiagnosticRequest` desacopla o motor do módulo ticket;
  `DiagnosticProvider` desacopla o motor do provider de IA.

## Módulos e limites

```
br.com.infocedro.clipper
├── ticket/      mundo do chamado: entidade, repositório, service, controller
├── clipper/     mundo do diagnóstico: agente, motor, contratos, provider de IA
├── knowledge/   base de conhecimento (KnowledgeArticle) — semente p/ RAG futuro
└── config/      configuração transversal (CORS, propriedades de IA)
```

A regra de ouro: o módulo `clipper` NÃO importa `Ticket`. A tradução
Ticket -> DiagnosticRequest acontece na borda (no controller), não dentro do
motor. É o que mantém o motor reaproveitável fora do contexto de tickets.

## O fluxo de diagnóstico

```
POST /api/tickets/{id}/diagnose
   |
   v
TicketController ----- a BORDA. Busca o Ticket e TRADUZ Ticket -> DiagnosticRequest.
   |                   (único ponto onde "ticket" e "clipper" se tocam)
   v
ClipperAgent -------- o rosto do motor. Recebe o contrato e repassa. Fino de propósito.
   |
   v
DiagnosticEngine ---- o ORQUESTRADOR. Não conhece Ticket (só o contrato) nem LLM
   |                   (só a interface). Hoje delega direto; ver "híbrido" abaixo.
   v
DiagnosticProvider -- a COSTURA (interface). "me dá um diagnóstico" sem dizer quem.
   |
   v
OpenAiCompatibleProvider -- fala HTTP com a API OpenAI-compatível (/chat/completions),
                            lê a resposta do modelo, devolve DiagnosticResult.
```

## Os padrões em ação (a parte de estudo)

Cada decisão acima tem um nome e um motivo. Estudar os nomes te ajuda a
reconhecer o mesmo padrão em qualquer projeto.

### 1. Inversão de dependência (o "D" do SOLID)

O `DiagnosticEngine` depende da **interface** `DiagnosticProvider`, não da classe
concreta `OpenAiCompatibleProvider`. Quem decide qual implementação entra é o
Spring (injeção de dependência), não o motor.

- **Problema que resolve:** se o motor chamasse o GPT direto, trocar de IA
  significaria reescrever o motor. Acoplamento rígido.
- **Como o código faz:** o construtor do `DiagnosticEngine` pede um
  `DiagnosticProvider` (a abstração). O detalhe concreto é injetado de fora.
- **Regra:** dependa de abstrações, não de implementações. Detalhes (o GPT)
  dependem da política (a interface), nunca o contrário.

### 2. Portas e adaptadores (arquitetura hexagonal, versão enxuta)

`DiagnosticProvider` é uma **porta**: um buraco na parede do domínio no formato
"me dê um diagnóstico". `OpenAiCompatibleProvider` é um **adaptador**: encaixa o
mundo externo (a API HTTP do GPT) naquele buraco.

- **Problema que resolve:** o domínio (diagnóstico) não deve saber que existe
  HTTP, JSON, ou OpenAI. Isso é detalhe de infraestrutura.
- **Como o código faz:** toda a sujeira de HTTP/JSON vive DENTRO do adaptador. O
  motor só enxerga a porta limpa.
- **Ganho:** Groq, DeepSeek, Ollama = outros adaptadores na mesma porta. Claude
  = um adaptador com formato de API diferente, mas na MESMA porta.

### 3. Contratos / DTOs (`DiagnosticRequest`, `DiagnosticResult`)

São `record`s simples: só dados, sem comportamento. Formam a "linguagem" que
atravessa a costura.

- **Problema que resolve:** se o motor recebesse a entidade `Ticket` (JPA, com
  id, status, anotações de banco), ele ficaria amarrado ao módulo ticket e ao
  banco. O contrato carrega SÓ o que o diagnóstico precisa (título, descrição).
- **Como o código faz:** `DiagnosticRequest` tem só title + description.
  `DiagnosticResult` tem causa, passos, confiança e fonte. Nada de Ticket.

### 4. Tradução na borda (barreira anti-corrupção)

No `TicketController`, a linha `new DiagnosticRequest(ticket.getTitle(), ...)` é o
ÚNICO ponto onde os dois mundos se tocam. É uma tradução deliberada.

- **Problema que resolve:** impede o modelo do ticket de "vazar" para dentro do
  domínio de diagnóstico. Cada mundo mantém seu vocabulário.
- **Termo formal:** Anti-Corruption Layer (do Domain-Driven Design).

### 5. Orquestrador magro

O `DiagnosticEngine` coordena, mas não executa o trabalho pesado. Ele fica entre
duas ignorâncias saudáveis: pra cima não sabe o que é um Ticket, pra baixo não
sabe qual IA responde.

- **Problema que resolve:** concentrar decisão (a ordem das etapas) num lugar,
  sem misturar com a execução (falar com o banco, falar com o LLM).
- **Futuro:** é aqui que vão entrar as regras determinísticas e a anonimização,
  sem inchar nenhuma outra classe.

### 6. Injeção de dependência via Spring

Nenhuma classe dá `new` na outra. O Spring monta o grafo: vê que
`DiagnosticEngine` precisa de um `DiagnosticProvider`, encontra o bean
`OpenAiCompatibleProvider` (anotado com `@Component`) e injeta no construtor.

- **Por que construtor e não campo:** dependências obrigatórias no construtor
  deixam a classe impossível de instanciar pela metade e fáceis de testar (você
  passa um provider falso no teste).

## A costura do motor de IA (`DiagnosticProvider`)

Tudo que fala com um LLM fica ATRÁS da interface `DiagnosticProvider`. Trocar de
motor é uma classe nova + escolher qual bean fica ativo — o resto (engine,
agente, fluxo do ticket, contratos) não muda.

Custo de cada provider nessa arquitetura:

| Provider              | Como entra                                              |
|-----------------------|---------------------------------------------------------|
| GPT-4o-mini (MVP)     | `OpenAiCompatibleProvider` — RestClient, zero dependência|
| Groq / DeepSeek / ...  | MESMA classe, só troca base-url + model + chave          |
| Ollama (local)        | MESMA classe, base-url local                             |
| Claude                | classe PRÓPRIA (não é OpenAI-compatível; SDK Java)      |

## Decisão de provider (MVP)

- **Default do MVP: GPT-4o-mini** via API OpenAI-compatível. Barato, infra
  confiável, API paga não treina nos dados (privacidade). **Groq** como fallback.
- **Por que OpenAI-compatível:** quase todos os providers falam o mesmo formato
  de API. Um cliente, troca base-url + chave + modelo. Fallback e múltiplas
  chaves ficam nativos na abstração.
- **Claude / cascata (Haiku -> Sonnet -> Opus): opção FUTURA**, não MVP. Segue
  válida se um dia o volume/criticidade justificar. Exige provider próprio.
- **Sem self-host / treino próprio** enquanto o volume for baixo.

## Privacidade e anonimização [PLANEJADO — ainda não implementado]

Etapa OBRIGATÓRIA e independente do provider: antes de mandar qualquer coisa pro
LLM, mascarar dado sensível (CNPJ / CPF / razão social / valores) em tokens
(ex: `[CNPJ_1]`), e des-mascarar localmente ao montar a nota pro técnico. Dado
real nunca sai da rede. Encaixa no `DiagnosticEngine`, entre a request e o
provider — bom exemplo de por que o orquestrador magro existe: a etapa nova
entra num lugar só.

## Fluxo híbrido [PARCIALMENTE PLANEJADO]

O `DiagnosticEngine` deve virar: regras determinísticas (`DiagnosticRule`) para
casos conhecidos -> anonimização -> IA para o resto. Hoje ele só delega pra IA;
as regras e a anonimização ainda são TODO.

## Persistência

- Hoje: H2 em arquivo com `MODE=PostgreSQL`, schema gerado pelo Hibernate
  (`ddl-auto=update`) enquanto o modelo ainda muda. Ponte de desenvolvimento.
- Gatilho de produção (profile `prod` com Postgres real): adicionar Flyway,
  trocar `ddl-auto` para `validate`, gerar migration de baseline, mover
  credenciais pra variáveis de ambiente. Ver `DebitoTecnico.local`.

## Configuração e segredos

- Propriedades de IA em `application.properties`, prefixo `clipper.ai.*`
  (base-url, model, api-key), lidas pelo record `AiProperties`.
- A **chave é server-side** e vem da variável de ambiente `OPENAI_API_KEY` —
  nunca versionada. Trocar de provider = mudar base-url + model + a chave.

## Estado atual vs planejado

| Item                                   | Estado      |
|----------------------------------------|-------------|
| CRUD de tickets + persistência JPA     | Feito       |
| Endpoint POST /tickets/{id}/diagnose   | Feito       |
| Costura DiagnosticProvider + engine    | Feito       |
| Provider GPT-4o-mini (OpenAI-compat)   | Feito       |
| Anonimização                           | Planejado   |
| Regras determinísticas (híbrido)       | Planejado   |
| RAG / base de conhecimento             | Semente     |
| Fallback multi-provider (Groq)         | Planejado   |
| Migração Postgres + Flyway             | Planejado   |

## Conceitos para aprofundar

Os nomes formais dos padrões usados aqui, para estudar por fora:

- **SOLID** — em especial o "D" (Dependency Inversion Principle).
- **Ports and Adapters / Arquitetura Hexagonal** — a ideia de porta
  (`DiagnosticProvider`) e adaptador (`OpenAiCompatibleProvider`).
- **DTO (Data Transfer Object)** — `DiagnosticRequest` / `DiagnosticResult`.
- **Anti-Corruption Layer (DDD)** — a tradução na borda do controller.
- **Injeção de dependência / Inversão de controle** — como o Spring monta tudo.
- **Strategy pattern** — trocar o "como diagnosticar" (o provider) sem mudar
  quem chama; é primo da porta/adaptador.

## Referências

- Decisões de arquitetura: memórias do projeto + arquivos `.local` na raiz.
- Metodologia de trabalho: `EscopoDoProjeto.local` (codar na mão, aprender).
- Dívida técnica: `DebitoTecnico.local`.
- Convenções de commit/release: `LeiDosCommits.local`, `RELEASE.local`.
