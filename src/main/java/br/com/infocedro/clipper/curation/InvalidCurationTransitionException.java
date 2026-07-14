package br.com.infocedro.clipper.curation;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Impede saltos ou ações que ainda não pertencem à etapa atual da curadoria. */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidCurationTransitionException extends RuntimeException {

    public InvalidCurationTransitionException(String message) {
        super(message);
    }
}
