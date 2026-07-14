package br.com.infocedro.clipper.catalog;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class RawDocumentNotFoundException extends RuntimeException {

    public RawDocumentNotFoundException(Long id) {
        super("Documento bruto não encontrado: " + id);
    }
}
