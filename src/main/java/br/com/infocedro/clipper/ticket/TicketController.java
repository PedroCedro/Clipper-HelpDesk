package br.com.infocedro.clipper.ticket;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import br.com.infocedro.clipper.clipper.ClipperAgent;
import br.com.infocedro.clipper.clipper.DiagnosisFeedback;
import br.com.infocedro.clipper.clipper.DiagnosticRequest;
import br.com.infocedro.clipper.clipper.DiagnosticResult;
import br.com.infocedro.clipper.curation.CurationCaseCreationResult;
import br.com.infocedro.clipper.curation.CurationCaseService;

@RestController
@RequestMapping("/api")
public class TicketController {

    private final TicketService ticketService;
    private final ClipperAgent clipperAgent;
    private final CurationCaseService curationCaseService;

    public TicketController(
            TicketService ticketService,
            ClipperAgent clipperAgent,
            CurationCaseService curationCaseService) {
        this.ticketService = ticketService;
        this.clipperAgent = clipperAgent;
        this.curationCaseService = curationCaseService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ATIVO", "service", "clipper");
    }

    @GetMapping("/tickets")
    public List<TicketResponse> listTickets() {
        // Composição na borda: cada ticket sai com o resumo do último
        // diagnóstico (a tag de IA da fila). Uma consulta por ticket é ok
        // no volume atual; quando doer, a costura pra otimizar é o agente
        // (buscar resumos em lote), sem mudar este contrato.
        return ticketService.findAll().stream()
                .map(ticket -> TicketResponse.of(
                        ticket,
                        clipperAgent.summaryFor(ticket.getId()).orElse(null)))
                .toList();
    }

    @PostMapping("/tickets")
    public Ticket createTicket(@RequestBody Ticket ticket) {
        return ticketService.createTicket(ticket);
    }

    @PostMapping("/tickets/{id}/diagnose")
    public Map<String, DiagnosticResult> diagnose(@PathVariable Long id) {
        // 1) busca o ticket (módulo ticket)
        Ticket ticket = ticketService.findById(id);

        // 2) TRADUÇÃO: Ticket -> DiagnosticRequest. É AQUI, na borda, que os dois mundos se encontram.
        DiagnosticRequest request = new DiagnosticRequest(ticket.getTitle(), ticket.getDescription());

        // 3) chama o motor (módulo clipper) só com o contrato. O agente
        //    também PERSISTE o resultado (upsert por ticket) — reabrir o
        //    ticket não gasta IA de novo.
        DiagnosticResult diagnosis = clipperAgent.analyze(ticket.getId(), request);

        return Map.of("diagnosis", diagnosis);
    }

    // ---- Ações do painel (rodada B3) ----

    // "Aplicar como resposta": o texto (em geral o diagnóstico do Clipper,
    // mas a API aceita qualquer resposta) vai pro solicitante e o chamado
    // encerra como RESOLVIDO. Resposta vazia é 400.
    @PostMapping("/tickets/{id}/reply")
    public Ticket reply(@PathVariable Long id, @RequestBody ReplyRequest request) {
        return ticketService.reply(id, request.response());
    }

    // "Escalar para humano": marca o chamado como ESCALADO. Sem corpo —
    // a nota de escalonamento entra quando existir fila de N2 pra lê-la.
    @PostMapping("/tickets/{id}/escalate")
    public Ticket escalate(@PathVariable Long id) {
        return ticketService.escalate(id);
    }

    // "Marcar diagnóstico incorreto": registra o feedback (módulo clipper)
    // com snapshot do diagnóstico atual. Valida o ticket ANTES pra manter a
    // semântica dos status: 404 = ticket não existe, 409 = existe mas nunca
    // foi diagnosticado. Devolve só o recibo — a entidade não sai pela API.
    @PostMapping("/tickets/{id}/diagnosis/feedback")
    public ResponseEntity<FeedbackReceipt> flagIncorrect(
            @PathVariable Long id, @RequestBody(required = false) FeedbackRequest request) {
        ticketService.findById(id);
        DiagnosisFeedback feedback = clipperAgent.flagIncorrect(id, request != null ? request.reason() : null);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new FeedbackReceipt(feedback.getId(), feedback.getCreatedAt()));
    }

    // DTOs de entrada/saída das ações — vivem na borda porque só existem
    // pra dar forma ao JSON; nenhum módulo interno os conhece.
    public record ReplyRequest(String response) {
    }

    public record FeedbackRequest(String reason) {
    }

    public record FeedbackReceipt(Long feedbackId, Instant createdAt) {
    }

    @PostMapping("/tickets/{id}/curation-case")
    public ResponseEntity<CurationCaseCreationResult> sendToCuration(
            @PathVariable Long id,
            @org.springframework.web.bind.annotation.RequestHeader("X-Actor") String actor,
            @RequestBody CurationRequest request) {
        ticketService.findById(id);
        CurationCaseCreationResult result = curationCaseService.createFromTicket(id, actor, request.reason());
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result);
    }

    public record CurationRequest(String reason) {
    }

    @GetMapping("/tickets/{id}/diagnosis")
    public ResponseEntity<Map<String, DiagnosticResult>> lastDiagnosis(@PathVariable Long id) {
        // Valida que o ticket existe (404 coerente com o resto da API)...
        ticketService.findById(id);

        // ...e devolve o último diagnóstico salvo. 204 quando nunca foi
        // diagnosticado: ausência não é erro, é estado legítimo do ticket.
        return clipperAgent.lastDiagnosis(id)
                .map(diagnosis -> ResponseEntity.ok(Map.of("diagnosis", diagnosis)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
