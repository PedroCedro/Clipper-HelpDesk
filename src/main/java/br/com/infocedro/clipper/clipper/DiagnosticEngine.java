package br.com.infocedro.clipper.clipper;

import org.springframework.stereotype.Component;

@Component
public class DiagnosticEngine {

    // Agora dependemos só do contrato DiagnosticRequest (mesmo pacote, sem import).
    // O motor não sabe mais o que é um "Ticket" — exatamente o que a costura buscava.
   public DiagnosticResult run(DiagnosticRequest request) {
        return new DiagnosticResult(
                "Initial diagnosis prepared for the ticket: " + request.title(),
                "Collect logs, check recent changes, and validate the affected module.",
                0.50,
                "stub"
        );
    }
}