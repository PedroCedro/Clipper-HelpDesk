package br.com.infocedro.clipper.curation;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Mantém o contrato de ausência do domínio independente da futura borda HTTP. */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class CurationCaseNotFoundException extends RuntimeException {

    public CurationCaseNotFoundException(Long id) {
        super("Caso de curadoria não encontrado: " + id);
    }
}
