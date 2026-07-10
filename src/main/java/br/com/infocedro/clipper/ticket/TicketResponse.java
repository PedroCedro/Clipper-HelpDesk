package br.com.infocedro.clipper.ticket;

import java.time.Instant;

import br.com.infocedro.clipper.clipper.ClipperAgent;

// Resposta da listagem de tickets: o ticket + o RESUMO do último
// diagnóstico (estado do gate + confiança) — é o que a fila do console
// precisa pra tag de IA, sem carregar causa/passos de cada um.
//
// DTO de borda de propósito: a entidade JPA não sai pela API, e a
// composição ticket+diagnóstico acontece aqui, onde os dois módulos já
// se encontram.
public record TicketResponse(
        Long id,
        String title,
        String description,
        String status,
        String priority,
        String requester,
        String routine,
        Instant createdAt,
        String response,
        ClipperAgent.DiagnosisSummary diagnosis) {

    // diagnosis nulo = ticket nunca diagnosticado — o front mostra a linha
    // sem tag de IA, que é a verdade.
    public static TicketResponse of(Ticket ticket, ClipperAgent.DiagnosisSummary diagnosis) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getRequester(),
                ticket.getRoutine(),
                ticket.getCreatedAt(),
                ticket.getResponse(),
                diagnosis);
    }
}
