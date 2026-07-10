package br.com.infocedro.clipper.ticket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

// Travam as ações do painel (B3): responder grava o texto E resolve o
// chamado; escalar só muda o status; resposta vazia é recusada antes de
// tocar o banco.
class TicketServiceTest {

    private TicketRepository repository;
    private TicketService service;
    private Ticket ticket;

    @BeforeEach
    void setUp() {
        repository = mock(TicketRepository.class);
        service = new TicketService(repository);

        ticket = new Ticket();
        ticket.setId(7L);
        ticket.setTitle("Cupom sumiu");
        ticket.setStatus("NOVO");
        when(repository.findById(7L)).thenReturn(Optional.of(ticket));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void responderGravaTextoEResolve() {
        Ticket saved = service.reply(7L, "Reprocessar os cupons na 2097.");

        assertEquals("Reprocessar os cupons na 2097.", saved.getResponse());
        assertEquals("RESOLVIDO", saved.getStatus());
        verify(repository).save(ticket);
    }

    @Test
    void respostaVaziaEhRecusadaSemTocarOBanco() {
        assertThrows(InvalidTicketActionException.class, () -> service.reply(7L, "   "));
        verify(repository, never()).save(any());
    }

    @Test
    void escalarSoMudaOStatus() {
        Ticket saved = service.escalate(7L);

        assertEquals("ESCALADO", saved.getStatus());
        // Escalar não inventa resposta — o chamado sobe como está.
        assertEquals(null, saved.getResponse());
    }

    @Test
    void acaoEmTicketInexistenteEh404() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(TicketNotFoundException.class, () -> service.escalate(99L));
    }
}
