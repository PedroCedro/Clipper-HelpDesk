# Changelog

Todas as mudanças relevantes deste projeto serao registradas neste arquivo.

O formato segue o padrao Keep a Changelog.

## [Unreleased]

### Added
- estrutura base do projeto Clipper Helpdesk
- backend Spring Boot com endpoint `GET /api/health`
- frontend React com componente placeholder do Clipper
- documentacao inicial do projeto
- persistencia de tickets com JPA (entidade `Ticket` e repositorio `TicketRepository`)
- endpoints `GET /api/tickets` e `POST /api/tickets`
- endpoint `POST /api/tickets/{id}/diagnose` para acionar o diagnostico do Clipper
- resultado de diagnostico estruturado (`DiagnosticResult`: causa provavel, proximos passos, confianca e fonte)
- persistencia em H2 em arquivo no modo PostgreSQL para desenvolvimento

### Changed
- clipper desacoplado do modulo ticket via contrato `DiagnosticRequest`
- motor de diagnostico passa a retornar `DiagnosticResult` em vez de `String`

### Fixed
- `moduleResolution` do tsconfig do frontend ajustado para `Bundler`
- busca de ticket inexistente passa a responder `404` em vez de `500`

