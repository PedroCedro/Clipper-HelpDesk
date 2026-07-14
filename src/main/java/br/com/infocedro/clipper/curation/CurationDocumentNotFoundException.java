package br.com.infocedro.clipper.curation;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Traduz a ausência no catálogo sem vazar uma exceção de outro módulo. */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class CurationDocumentNotFoundException extends RuntimeException {

    public CurationDocumentNotFoundException(Long documentId) {
        super("Documento candidato não encontrado: " + documentId);
    }
}
