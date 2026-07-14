package br.com.infocedro.clipper.curation;

import java.time.OffsetDateTime;

/** Vínculo ativo apresentado sem expor a entidade JPA. */
public record CurationCandidateView(
        Long id, Long documentId, String author, String reason, OffsetDateTime createdAt
) {
    static CurationCandidateView from(CurationCandidate candidate) {
        return new CurationCandidateView(
                candidate.getId(), candidate.getDocumentId(), candidate.getAuthor(),
                candidate.getReason(), candidate.getCreatedAt());
    }
}
