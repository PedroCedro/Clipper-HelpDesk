package br.com.infocedro.clipper.collector;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** Resultado comum de uma execução, independente da tecnologia da fonte. */
public record CollectionSummary(
        String sourceType,
        String scope,
        int containers,
        int documents,
        int discarded,
        int errors,
        Path outputDirectory,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        Map<String, Object> parameters,
        List<Path> artifacts
) {
    public CollectionSummary {
        parameters = Map.copyOf(parameters);
        artifacts = List.copyOf(artifacts);
    }
}
