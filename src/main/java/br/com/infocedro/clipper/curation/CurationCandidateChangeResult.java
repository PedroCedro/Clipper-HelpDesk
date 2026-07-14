package br.com.infocedro.clipper.curation;

/** Resultado idempotente com a contagem real, sem inferi-la pelo estado do caso. */
public record CurationCandidateChangeResult(
        Long caseId,
        Long documentId,
        boolean changed,
        long candidateCount,
        CurationStatus caseStatus
) {
}
