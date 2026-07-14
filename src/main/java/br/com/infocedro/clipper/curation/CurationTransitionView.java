package br.com.infocedro.clipper.curation;

import java.time.OffsetDateTime;

/** Entrada pública do histórico de estados. */
public record CurationTransitionView(
        Long id,
        CurationStatus fromStatus,
        CurationStatus toStatus,
        String actor,
        String reason,
        OffsetDateTime createdAt
) {
    static CurationTransitionView from(CurationCaseTransition transition) {
        return new CurationTransitionView(
                transition.getId(), transition.getFromStatus(), transition.getToStatus(),
                transition.getActor(), transition.getReason(), transition.getCreatedAt());
    }
}
