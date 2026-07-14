package br.com.infocedro.clipper.curation;

/** Dados seguros do catálogo necessários para reconhecer uma fonte na curadoria. */
public record CurationCatalogDocument(
        Long id, String title, String module, String sourceLabel, String sourceUrl
) {
}
