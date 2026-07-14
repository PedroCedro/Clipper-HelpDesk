package br.com.infocedro.clipper.curation;

/** Dados necessários para abrir um caso e registrar sua autoria. */
public record CreateCurationCaseCommand(
        CurationOriginType originType,
        String originReference,
        String actor,
        String reason
) {
}
