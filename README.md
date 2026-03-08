# Clipper Helpdesk

![Clipper Office](docs/clipper-office.png)

Clipper Helpdesk e um sistema de atendimento projetado para receber chamados de suporte e executar um diagnostico automatizado inicial antes da intervencao de um tecnico humano.

O assistente de automacao se chama `Clipper`. Ele le novos tickets, interpreta o contexto do problema, aplica regras de diagnostico e prepara um resumo inicial para acelerar a atuacao da equipe tecnica.

## Visao Geral

O objetivo do projeto e transformar o primeiro atendimento em um fluxo mais inteligente:

- o usuario abre um ticket
- o `Clipper` executa uma analise automatica inicial
- o sistema sugere causas provaveis e proximos passos
- o tecnico humano recebe o caso com mais contexto e menos triagem manual

Isso reduz o tempo gasto nas etapas repetitivas e prepara o sistema para evoluir para automacoes mais avancadas no futuro.

## O Papel do Clipper

O `Clipper` e o nucleo de automacao da plataforma. Ele foi pensado para atuar antes da triagem humana, ajudando a classificar o chamado e levantar hipoteses iniciais.

Responsabilidades esperadas:

- ler tickets de suporte recem-criados
- executar regras de diagnostico predefinidas
- produzir um resumo diagnostico inicial
- sugerir artigos relacionados da base de conhecimento
- preparar tickets para triagem humana

## Stack Tecnologica

- Backend: Java 21, Spring Boot, Maven, PostgreSQL, API REST
- Frontend: React, Vite, TypeScript

## Objetivos do Projeto

- Estrutura inspirada em clean architecture
- Pacotes modulares por capacidade de negocio
- Preparado para uma futura camada de automacao centrada no `Clipper`

## Estrutura do Repositorio

```text
clipper-helpdesk/
├── README.md
├── docs/
│   └── clipper-office.png
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/clipper
└── frontend/
    ├── package.json
    └── src/
```

Este repositorio contem a estrutura inicial para essa evolucao, com classes placeholder para o dominio de tickets e para o motor de diagnostico.

## Como Executar o Backend

Requisitos:

- Java 21
- Maven 3.9+
- PostgreSQL disponivel para integracao futura

Execucao:

```bash
cd backend
mvn spring-boot:run
```

Endpoint de saude:

```text
GET /api/health
```

## Como Executar o Frontend

Requisitos:

- Node.js 20+

Execucao:

```bash
cd frontend
npm install
npm run dev
```
