package br.com.infocedro.clipper.curation;

import java.time.OffsetDateTime;
import java.util.List;

/** Visão completa necessária para abrir um caso na interface de curadoria. */
public record CurationCaseDetail(
        Long id,
        CurationOriginType originType,
        String originReference,
        CurationStatus status,
        String reason,
        String author,
        long candidateCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<CurationCandidateView> candidates,
        List<CurationTransitionView> transitions,
        List<CurationCandidateEventView> candidateEvents
) {
}
