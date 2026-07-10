package br.com.infocedro.clipper.ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class TicketRepositoryTest {

    @Autowired
    private TicketRepository repository;

    @Test
    void ticketSemStatusEhSalvoComoNovo() {
        Ticket ticket = new Ticket();
        ticket.setTitle("Ticket sem status");
        ticket.setDescription("Status deve receber o default no insert");

        Ticket saved = repository.saveAndFlush(ticket);

        assertEquals("NOVO", saved.getStatus());
    }
}
