package br.com.infocedro.clipper.clipper;

import org.springframework.stereotype.Component;

@Component
public class ClipperAgent {

    private final DiagnosticEngine diagnosticEngine;

    public ClipperAgent(DiagnosticEngine diagnosticEngine) {
        this.diagnosticEngine = diagnosticEngine;
    }

    // Recebe o contrato e repassa pro motor. O agente também não conhece mais Ticket.
    public DiagnosticResult analyze(DiagnosticRequest request) {
        return diagnosticEngine.run(request);
    }
}
