package br.com.infocedro.clipper.ticket;

import java.util.List;
import org.springframework.stereotype.Service;

// Concentra as regras de acesso a tickets para o controller não falar direto
// com o repositório em todos os fluxos.
@Service
public class TicketService {

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public List<Ticket> findAll() {
        return ticketRepository.findAll();
    }

    public Ticket createTicket(Ticket ticket) {
        return ticketRepository.save(ticket);
    }

    public Ticket findById(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
    }
}
