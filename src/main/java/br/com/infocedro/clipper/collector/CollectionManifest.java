package br.com.infocedro.clipper.collector;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/** Snapshot auditável de uma execução e dos arquivos que ela produziu. */
public record CollectionManifest(
        int schemaVersion,
        String applicationVersion,
        String collectionId,
        String sourceType,
        String scope,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        int containers,
        int documents,
        int discarded,
        int errors,
        Map<String, Object> parameters,
        List<Artifact> artifacts
) {
    public record Artifact(String path, long sizeBytes, String sha256) {
    }
}
