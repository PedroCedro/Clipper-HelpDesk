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
- base de conhecimento curada (`KnowledgeArticle`, `KnowledgeRepository`) com seed em YAML de 3 artigos sobre NFC-e em contingência
- busca por palavra-chave na base de conhecimento (`KnowledgeSearch`), com limiar mínimo de tokens para considerar um match forte
- testes de unidade do retrieval (`KnowledgeSearchTest`, trava o limiar de match forte) e do contrato do gate de grounding no motor (`DiagnosticEngineTest`: ancorado não chama IA; sem base a IA só recebe texto mascarado)
- fluxo "apoiado" (RAG-lite): com match fraco na base, o artigo curado vai de material de apoio no prompt da IA via contrato `KnowledgeContext`, e o `source` marca `apoiado: <artigo> · via <modelo>`

### Changed
- clipper desacoplado do módulo ticket via contrato `DiagnosticRequest`
- motor de diagnóstico passa a retornar `DiagnosticResult` em vez de `String`
- `DiagnosticEngine` passa a orquestrar delegando ao `DiagnosticProvider` (antes devolvia um diagnóstico fixo)
- `DiagnosticEngine` aplica fluxo híbrido: consulta a base de conhecimento antes da IA; com match forte devolve o artigo curado (`source` = `ancorado: ...`, sem chamada de IA); sem match cai na IA e marca `source` = `sem-base: ...`
- `KnowledgeSearch` passa a classificar a força do match (`KnowledgeMatch`): 2+ tokens = forte (lookup puro), 1 token = fraco (apoio pra IA) — antes o match de 1 token era descartado
- `DiagnosticProvider.diagnose` passa a aceitar material de apoio opcional; os dois providers (OpenAI-compatível e Claude) incluem o artigo no prompt quando presente
- frontend reconstruído como console: `Dashboard`/`ClipperWidget` dão lugar a `Console` + componentes pequenos (Sidebar, Topbar, TicketQueue, TicketDetail, ClipperPanel, NewTicketForm); ações do painel nascem desabilitadas até existirem endpoints (rodada B3)
- `GET /api/tickets` passa a devolver `TicketResponse` (DTO de borda) com o resumo do último diagnóstico (estado do gate + confiança) — a fonte da tag de IA na fila
- `ClipperAgent` vira o caso de uso do diagnóstico: analisa e persiste (upsert); o `DiagnosticEngine` continua puro, sem conhecer banco
- teto de confiança por estado do gate: a autoavaliação da IA é cortada em 0.9 no estado `apoiado` e 0.5 no `sem-base` (só artigo curado verbatim vale 1.0); teto, não piso — autoavaliação baixa é respeitada
- gate de grounding estruturado na resposta da API (`grounding`: estado `ANCORADO`/`APOIADO`/`SEM_BASE` + título/URL do artigo + modelo) — a string `source` vira leitura humana/log, a UI não a parseia
- widget do frontend renderiza o gate: selo por estado (rótulos neutros), barra de confiança e link "ver fonte oficial" quando há artigo de base
- console do agente (3 zonas): sidebar com marca ticket+pulso, fila master-detail de tickets, detalhe com painel "Diagnóstico do Clipper" (faixa do gate por estado, confiança, fonte, legenda explicando os selos)
- tema claro/escuro/sistema com tokens de design (persistido em localStorage)
- busca client-side na fila (nº, título, descrição) e formulário mínimo de novo ticket (alimenta a demo sem curl)
- diagnóstico persistido por ticket (`Diagnosis`, uma linha por ticket — rediagnosticar substitui); reabrir ticket não gasta IA de novo
- `GET /api/tickets/{id}/diagnosis` devolve o último diagnóstico salvo (204 quando o ticket nunca foi diagnosticado)
- ticket ganha `createdAt` (carimbado na inserção), `priority`, `requester` e `routine`
- textos pt-br (README, CHANGELOG, UI do frontend e comentários) passam a usar acentuação completa

### Fixed
- `moduleResolution` do tsconfig do frontend ajustado para `Bundler`
- busca de ticket inexistente passa a responder `404` em vez de `500`
- busca da base de conhecimento não casa mais por pedaço de keyword (token "144" não ancora mais no artigo da rotina 1443) — o ponto só vale se o token for palavra inteira das keywords
