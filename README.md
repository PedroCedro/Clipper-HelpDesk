# Clipper

![Clipper Office](docs/clipper-office.png)

Clipper é um sistema de atendimento projetado para receber chamados de suporte e executar um diagnóstico automatizado inicial antes da intervenção de um técnico humano.

O assistente de automação se chama `Clipper`. Ele lê novos tickets, interpreta o contexto do problema, aplica regras de diagnóstico e prepara um resumo inicial para acelerar a atuação da equipe técnica.

## Visão Geral

O objetivo do projeto é transformar o primeiro atendimento em um fluxo mais inteligente:

- o usuário abre um ticket
- o `Clipper` executa uma análise automatizada inicial
- o sistema sugere causas prováveis e próximos passos
- o técnico humano recebe o caso com mais contexto e menos triagem manual

Isso reduz o tempo gasto nas etapas repetitivas e prepara o sistema para evoluir para automações mais avançadas no futuro.

## O Papel do Clipper

O `Clipper` é o núcleo de automação da plataforma. Ele foi pensado para atuar antes da triagem humana, ajudando a classificar o chamado e levantar hipóteses iniciais.

Responsabilidades esperadas:

- ler tickets de suporte recém-criados
- executar regras de diagnóstico predefinidas
- produzir um resumo diagnóstico inicial
- sugerir artigos relacionados da base de conhecimento
- preparar tickets para triagem humana

## Stack Tecnológica

- Backend: Java 21, Spring Boot, Maven Wrapper, PostgreSQL, API REST
- Frontend: React, Vite, TypeScript

## Objetivos do Projeto

- Estrutura inspirada em clean architecture
- Pacotes modulares por capacidade de negócio
- Preparado para uma futura camada de automação centrada no `Clipper`

## Estrutura do Repositório

```text
clipper/
├── README.md
├── docs/
│   └── clipper-office.png
├── pom.xml
├── src/
│   └── main/java/br/com/infocedro/clipper
└── frontend/
    ├── package.json
    └── src/
```

Este repositório contém a base atual do projeto, com backend Spring Boot estruturado pelo Spring Initializr e frontend React/Vite separado em `frontend/`.

## Como Executar o Backend

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
GET /api/health
GET /api/tickets
```

## Como Executar o Frontend

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

## Estado Atual

- o backend novo roda na raiz do projeto com `pom.xml`, `src/` e `mvnw.cmd`
- o frontend foi consolidado em `frontend/`
- a pasta antiga `Clipper-HelpDesk` foi descontinuada como base principal
