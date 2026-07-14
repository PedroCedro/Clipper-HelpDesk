package br.com.infocedro.clipper.catalog;

/** Projeção leve para consumidores que não precisam do corpo do documento. */
interface RawDocumentSummaryProjection {
    Long getId();
    String getSourceType();
    String getTitle();
    String getModule();
    String getSourceUrl();
}
