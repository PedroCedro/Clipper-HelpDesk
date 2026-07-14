package br.com.infocedro.clipper.curation;

import java.time.OffsetDateTime;

/** Entrada pública da trilha de associação e remoção. */
public record CurationCandidateEventView(
        Long id,
        Long documentId,
        CurationCandidateEventType eventType,
        String actor,
        String reason,
        OffsetDateTime createdAt
) {
    static CurationCandidateEventView from(CurationCandidateEvent event) {
        return new CurationCandidateEventView(
                event.getId(), event.getDocumentId(), event.getEventType(),
                event.getActor(), event.getReason(), event.getCreatedAt());
    }
}
