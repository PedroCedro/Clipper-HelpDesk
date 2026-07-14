package br.com.infocedro.clipper.catalog;

/** Identificação segura de uma fonte sem transportar conteúdo ou metadata. */
public record RawDocumentSummary(
        Long id, String title, String module, String sourceLabel, String sourceUrl
) {
}
