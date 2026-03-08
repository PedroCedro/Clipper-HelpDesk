# Clipper Helpdesk

![Clipper Office](docs/clipper-office.png)

Clipper Helpdesk é um sistema de atendimento projetado para receber chamados de suporte e executar um diagnóstico automatizado inicial antes da intervenção de um técnico humano.

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

## Stack Tecnologica

- Backend: Java 21, Spring Boot, Maven, PostgreSQL, API REST
- Frontend: React, Vite, TypeScript

## Objetivos do Projeto

- Estrutura inspirada em clean architecture
- Pacotes modulares por capacidade de negócio
- Preparado para uma futura camada de automacao centrada no `Clipper`

## Estrutura do Repositório

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

Este repositório contém a estrutura inicial para essa evolução, com classes placeholder para o domínio de tickets e para o motor de diagnóstico.

## Como Executar o Backend

Requisitos:

- Java 21
- Maven 3.9+
- PostgreSQL disponível para integração futura

Execução:

```bash
cd backend
mvn spring-boot:run
```

Endpoint de saúde:

```text
GET /api/health
```

## Como Executar o Frontend

Requisitos:

- Node.js 20+

Execução:

```bash
cd frontend
npm install
npm run dev
```
