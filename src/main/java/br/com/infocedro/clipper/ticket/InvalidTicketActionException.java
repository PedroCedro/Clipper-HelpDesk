package br.com.infocedro.clipper.ticket;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

// Ação inválida sobre um ticket (ex.: aplicar resposta vazia). Vira 400
// na borda — mesmo mecanismo do TicketNotFoundException com o 404.
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidTicketActionException extends RuntimeException {
    public InvalidTicketActionException(String message) {
        super(message);
    }
}
