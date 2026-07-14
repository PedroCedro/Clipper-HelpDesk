package br.com.infocedro.clipper.collector;

import java.time.OffsetDateTime;

/** Resultado comum de uma execução, independente da tecnologia da fonte. */
public record CollectionSummary(
        String sourceType,
        int containers,
        int documents,
        String outputLocation,
        OffsetDateTime collectedAt
) {
}
