# Changelog

Todas as mudanças relevantes deste projeto serão registradas neste arquivo.

O formato segue o padrão Keep a Changelog.

## [Unreleased]

### Added
- estrutura base do projeto Clipper Helpdesk
- backend Spring Boot com endpoint `GET /api/health`
- frontend React com componente placeholder do Clipper
- documentação inicial do projeto
- persistência de tickets com JPA (entidade `Ticket` e repositório `TicketRepository`)
- endpoints `GET /api/tickets` e `POST /api/tickets`
- endpoint `POST /api/tickets/{id}/diagnose` para acionar o diagnóstico do Clipper
- resultado de diagnóstico estruturado (`DiagnosticResult`: causa provável, próximos passos, confiança e fonte)
- persistência em H2 em arquivo no modo PostgreSQL para desenvolvimento
- motor de diagnóstico por IA atrás da interface `DiagnosticProvider`, com `OpenAiCompatibleProvider` (API OpenAI-compatível, GPT-4o-mini por padrão) e configuração em `clipper.ai.*`
- documento de arquitetura (`ARQUITETURA.md`)

### Changed
- clipper desacoplado do módulo ticket via contrato `DiagnosticRequest`
- motor de diagnóstico passa a retornar `DiagnosticResult` em vez de `String`
- `DiagnosticEngine` passa a orquestrar delegando ao `DiagnosticProvider` (antes devolvia um diagnóstico fixo)
- textos pt-br (README, CHANGELOG, UI do frontend e comentários) passam a usar acentuação completa

### Fixed
- `moduleResolution` do tsconfig do frontend ajustado para `Bundler`
- busca de ticket inexistente passa a responder `404` em vez de `500`
