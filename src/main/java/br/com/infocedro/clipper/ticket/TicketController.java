package br.com.infocedro.clipper.ticket;

import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import br.com.infocedro.clipper.clipper.ClipperAgent;
import br.com.infocedro.clipper.clipper.DiagnosticRequest;
import br.com.infocedro.clipper.clipper.DiagnosticResult;

@RestController
@RequestMapping("/api")
public class TicketController {

    private final TicketService ticketService;
    private final ClipperAgent clipperAgent;

    public TicketController(TicketService ticketService, ClipperAgent clipperAgent) {
        this.ticketService = ticketService;
        this.clipperAgent = clipperAgent;
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
