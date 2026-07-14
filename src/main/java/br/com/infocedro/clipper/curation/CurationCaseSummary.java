package br.com.infocedro.clipper.curation;

import java.time.OffsetDateTime;

/** Item leve da fila, com contagem real em vez de inferência pelo estado. */
public record CurationCaseSummary(
        Long id,
        CurationOriginType originType,
        String originReference,
        CurationStatus status,
        String reason,
        String author,
        long candidateCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
