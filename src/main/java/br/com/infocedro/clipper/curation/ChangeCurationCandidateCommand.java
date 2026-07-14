package br.com.infocedro.clipper.curation;

/** Autoria e justificativa de uma associação ou remoção de candidato. */
public record ChangeCurationCandidateCommand(String actor, String reason) {
}
