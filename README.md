# Clipper

![Clipper](docs/clipper-office.png)

> Um assistente de clipe que, desta vez, aprendeu a ajudar de verdade.

Clipper é um sistema de atendimento (helpdesk) projetado para receber chamados de suporte e executar um diagnóstico automatizado inicial antes da intervenção de um técnico humano.

O assistente de automação se chama `Clipper`. Ele lê novos tickets, interpreta o contexto do problema, aplica regras de diagnóstico e — quando necessário — recorre à IA para sugerir causas prováveis e próximos passos, sempre preparando um resumo inicial para acelerar a atuação da equipe técnica.

## Visão geral

O objetivo do projeto é transformar o primeiro atendimento em um fluxo mais inteligente:

- o usuário abre um ticket
- o `Clipper` executa uma análise automatizada inicial (regras + IA)
- o sistema sugere causas prováveis e próximos passos, com nível de confiança e a fonte
- o técnico humano recebe o caso com mais contexto e menos triagem manual

Isso reduz o tempo gasto nas etapas repetitivas e prepara o sistema para evoluir para automações mais avançadas no futuro.

## O papel do Clipper

O `Clipper` é o núcleo de automação da plataforma. Ele foi pensado para atuar antes da triagem humana, ajudando a classificar o chamado e a levantar hipóteses iniciais.

Responsabilidades esperadas:

- ler tickets de suporte recém-criados
- executar regras de diagnóstico predefinidas
- recorrer à IA para os casos que as regras não cobrem
- produzir um resumo diagnóstico inicial, com confiança e fonte
- sugerir artigos relacionados da base de conhecimento
- preparar tickets para triagem humana

## Arquitetura

- **Motor híbrido:** regras determinísticas para os casos conhecidos e IA para o restante. O `DiagnosticEngine` atua como orquestrador.
- **IA atrás de uma interface (`DiagnosticProvider`), sem lock-in de fornecedor.** O provider padrão fala o formato compatível com a API da OpenAI, o que permite trocar de modelo/fornecedor apenas por configuração.
- **Pacotes modulares por capacidade de negócio** (monólito modular, com costuras para evoluir sem reescrever o núcleo).
- **Privacidade por princípio:** anonimização de dados sensíveis antes de qualquer chamada externa *(em construção)*.

## Stack tecnológica

- Backend: Java 21, Spring Boot, Maven Wrapper, API REST
- Frontend: React, Vite, TypeScript
- Diagnóstico: motor híbrido (regras + IA via provider configurável, compatível com o formato OpenAI por padrão)
- Persistência: PostgreSQL previsto para integração futura

## Objetivos do projeto

- Estrutura inspirada em clean architecture
- Pacotes modulares por capacidade de negócio
- Motor de IA atrás de interface, sem lock-in de fornecedor
- Base de conhecimento própria para ancorar o diagnóstico em fontes confiáveis
- Quando não há base confiável para o caso, sinalizar em vez de arriscar um palpite

## Estrutura do repositório

```text
clipper/
├── README.md
├── docs/
│   └── clipper-office.png
├── pom.xml
├── src/
│   └── main/java/br/com/infocedro/clipper
│       ├── clipper/      # motor de diagnóstico e providers
│       ├── ticket/       # chamados (API, serviço, repositório)
│       └── knowledge/    # base de conhecimento
└── frontend/
    ├── package.json
    └── src/
```

Este repositório contém a base atual do projeto, com backend Spring Boot estruturado pelo Spring Initializr e frontend React/Vite separado em `frontend/`.

## Como executar o backend

Requisitos:

- Java 21
- Git Bash ou PowerShell
- PostgreSQL disponível para integração futura

Execução:

```powershell
.\mvnw.cmd spring-boot:run
```

Endpoints iniciais:

```text
GET  /api/health
GET  /api/tickets
POST /api/tickets
POST /api/tickets/{id}/diagnose
```

## Como executar o frontend

Requisitos:

- Node.js 20+

Execução:

```powershell
cd frontend
npm install
npm run dev
```

Observações de desenvolvimento local:

- o frontend espera o backend em `http://localhost:8080`
- o Vite faz proxy automático de `/api` para o backend durante `npm run dev`
- se preferir chamar a API Spring diretamente no navegador, o backend libera CORS para `http://localhost:5173`

## Como executar o coletor

O coletor fica desligado durante a execução normal da API. Cada rodada grava
JSONL e um manifesto com hashes em `var/knowledge/raw/<fonte>/<módulo>/<execução>/`.
Os arquivos brutos são locais e não alimentam o diagnóstico antes da curadoria.

Coletar um módulo com limites de desenvolvimento:

```powershell
.\src\main\java\br\com\infocedro\clipper\collector\run-crawler.ps1 `
  -Modulo 14-faturamento -MaxSections 1 -MaxArticles 10
```

Coletar todos os módulos catalogados:

```powershell
.\src\main\java\br\com\infocedro\clipper\collector\run-crawler.ps1 -All
```

O catálogo de módulos fica em
`src/main/resources/collector/totvs-winthor-modules.yaml`. A busca exploratória
na coleta mais recente de um módulo pode ser feita com:

```powershell
.\src\main\java\br\com\infocedro\clipper\collector\search-local.ps1 `
  -Modulo 14-faturamento -Query "rejeição 1026"
```

## Estado atual

- o backend roda na raiz do projeto com `pom.xml`, `src/` e `mvnw.cmd`
- o frontend foi consolidado em `frontend/`
- o diagnóstico já roda via provider de IA configurável, combinado com as regras determinísticas
- o coletor organiza fontes brutas por módulo e registra manifesto auditável com SHA-256
- a pasta antiga `Clipper-HelpDesk` foi descontinuada como base principal
