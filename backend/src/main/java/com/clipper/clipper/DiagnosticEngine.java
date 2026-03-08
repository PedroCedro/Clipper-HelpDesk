package com.clipper.clipper;

import com.clipper.ticket.Ticket;
import org.springframework.stereotype.Component;

@Component
public class DiagnosticEngine {

    public String run(Ticket ticket) {
        return "Diagnostico inicial preparado para o ticket: " + ticket.getTitle();
    }
}
