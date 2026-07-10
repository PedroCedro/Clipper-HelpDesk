package br.com.infocedro.clipper.clipper;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Marcar como incorreto um diagnóstico que não existe é pedido inválido:
// 409 (conflito de estado), não 404 — o TICKET existe, o que falta é o
// diagnóstico. O front nem oferece o botão nesse caso, mas a API não
// confia em front.
@ResponseStatus(HttpStatus.CONFLICT)
public class DiagnosisNotFoundException extends RuntimeException {
    public DiagnosisNotFoundException(Long ticketId) {
        super("Ticket sem diagnóstico para marcar: " + ticketId);
    }
}
