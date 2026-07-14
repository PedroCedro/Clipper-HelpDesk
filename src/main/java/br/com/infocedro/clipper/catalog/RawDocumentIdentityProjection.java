package br.com.infocedro.clipper.catalog;

/** Identidade e hash suficientes para decidir se a migração precisa carregar o TEXT. */
interface RawDocumentIdentityProjection {
    Long getId();
    String getExternalId();
    String getContentHash();
}
