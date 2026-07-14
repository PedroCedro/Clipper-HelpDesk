package br.com.infocedro.clipper.curation;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Rejeita dados que não formam um caso de curadoria auditável. */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidCurationCaseException extends RuntimeException {

    public InvalidCurationCaseException(String message) {
        super(message);
    }
}
