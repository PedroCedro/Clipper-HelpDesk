package com.clipper.ticket;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ATIVO", "service", "clipper-helpdesk");
    }

    @GetMapping("/tickets")
    public List<Ticket> listTickets() {
        return ticketService.findAll();
    }
}
