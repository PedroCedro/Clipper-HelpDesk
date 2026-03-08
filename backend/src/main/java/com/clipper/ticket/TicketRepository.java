package com.clipper.ticket;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Repository;

@Repository
public class TicketRepository {

    private final List<Ticket> tickets = new CopyOnWriteArrayList<>();

    public TicketRepository() {
        tickets.add(new Ticket(
                1L,
                "Erro ao abrir o sistema",
                "Usuário relata falha ao iniciar o módulo principal.",
                "NOVO"
        ));
    }

    public List<Ticket> findAll() {
        return new ArrayList<>(tickets);
    }
}
