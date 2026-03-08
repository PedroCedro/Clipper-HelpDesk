package com.clipper.clipper;

import com.clipper.ticket.Ticket;
import org.springframework.stereotype.Component;

@Component
public class ClipperAgent {

    private final DiagnosticEngine diagnosticEngine;

    public ClipperAgent(DiagnosticEngine diagnosticEngine) {
        this.diagnosticEngine = diagnosticEngine;
    }

    public String analyze(Ticket ticket) {
        return diagnosticEngine.run(ticket);
    }
}
