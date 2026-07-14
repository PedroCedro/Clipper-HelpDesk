package br.com.infocedro.clipper.catalog;

import com.fasterxml.jackson.databind.JsonNode;

/** Traduz um formato externo para o contrato neutro do catálogo. */
public interface RawDocumentAdapter {

    boolean supports(String sourceType, int schemaVersion);

    RawDocumentCandidate adapt(JsonNode document, RawImportContext context);
}
