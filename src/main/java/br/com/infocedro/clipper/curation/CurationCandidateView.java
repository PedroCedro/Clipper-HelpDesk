package br.com.infocedro.clipper.curation;

import java.time.OffsetDateTime;

/** Vínculo ativo apresentado sem expor a entidade JPA. */
public record CurationCandidateView(
        Long id,
        Long documentId,
        String title,
        String module,
        String sourceLabel,
        String sourceUrl,
        String author,
        String reason,
        OffsetDateTime createdAt
) {
    static CurationCandidateView from(CurationCandidate candidate, CurationCatalogDocument document) {
        return new CurationCandidateView(
                candidate.getId(), candidate.getDocumentId(), document.title(), document.module(),
                document.sourceLabel(), document.sourceUrl(), candidate.getAuthor(), candidate.getReason(),
                candidate.getCreatedAt());
    }
}
