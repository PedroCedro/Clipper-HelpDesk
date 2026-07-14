package br.com.infocedro.clipper.curation;

/** Diferencia criação de reutilização para a borda responder idempotentemente. */
public record CurationCaseCreationResult(CurationCaseSnapshot curationCase, boolean created) {
}
