package br.com.infocedro.clipper.ticket;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import br.com.infocedro.clipper.clipper.ClipperAgent;
import br.com.infocedro.clipper.clipper.DiagnosticRequest;


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
    public List<Ticket> listTickets() {
        return ticketService.findAll();
    }

    @PostMapping("/tickets")
    public Ticket createTicket(@RequestBody Ticket ticket) {
        return ticketService.createTicket(ticket);
    }

    @PostMapping("/tickets/{id}/diagnose")
    public Map<String, String> diagnose(@PathVariable Long id) {
    // 1) busca o ticket (módulo ticket)
    Ticket ticket = ticketService.findById(id);

    // 2) TRADUÇÃO: Ticket -> DiagnosticRequest. É AQUI, na borda, que os dois mundos se encontram.
    DiagnosticRequest request = new DiagnosticRequest(ticket.getTitle(), ticket.getDescription());

    // 3) chama o motor (módulo clipper) só com o contrato
    String diagnosis = clipperAgent.analyze(request);

    return Map.of("diagnosis", diagnosis);
}

}
