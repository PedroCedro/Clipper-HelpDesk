package br.com.infocedro.clipper.clipper;

import org.springframework.stereotype.Component;

@Component
public class DiagnosticEngine {

    // Agora dependemos só do contrato DiagnosticRequest (mesmo pacote, sem import).
    // O motor não sabe mais o que é um "Ticket" — exatamente o que a costura buscava.
    public String run(DiagnosticRequest request) {
        return "Diagnostico inicial preparado para o ticket: " + request.title();
    }
}
