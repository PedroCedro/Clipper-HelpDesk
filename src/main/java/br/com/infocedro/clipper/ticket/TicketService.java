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

    // "Aplicar como resposta": grava o texto que vai pro solicitante e dá o
    // chamado por resolvido. Responder de novo SUBSTITUI (mesma filosofia do
    // diagnóstico: o console mostra a resposta atual; histórico é decisão
    // futura). Status como String de propósito — a transição vira máquina de
    // estados quando houver regra de quem-pode-ir-pra-onde.
    public Ticket reply(Long id, String response) {
        if (response == null || response.isBlank()) {
            throw new InvalidTicketActionException("Resposta não pode ser vazia.");
        }
        Ticket ticket = findById(id);
        ticket.setResponse(response);
        ticket.setStatus("RESOLVIDO");
        return ticketRepository.save(ticket);
    }

    // "Escalar para humano": o Clipper sai de cena e o chamado sobe de nível.
    // Por ora é só a marca no status — fila/atribuição de N2 é rodada futura.
    public Ticket escalate(Long id) {
        Ticket ticket = findById(id);
        ticket.setStatus("ESCALADO");
        return ticketRepository.save(ticket);
    }
}
