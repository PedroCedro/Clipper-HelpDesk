package br.com.infocedro.clipper.collector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CollectionManifestWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void gravaManifestoComHashTamanhoEParametros() throws Exception {
        Path artifact = tempDir.resolve("documents.jsonl");
        Files.writeString(artifact, "conteúdo\n");
        OffsetDateTime startedAt = OffsetDateTime.parse("2026-07-14T14:30:00-03:00");
        OffsetDateTime finishedAt = OffsetDateTime.parse("2026-07-14T14:31:00-03:00");
        CollectionSummary summary = new CollectionSummary(
                "totvs-winthor", "14-faturamento", 2, 10, 1, 0, tempDir,
                startedAt, finishedAt, Map.of("locale", "pt-br"), List.of(artifact));
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

        Path result = new CollectionManifestWriter(objectMapper, new ApplicationVersion()).write(summary);

        JsonNode manifest = objectMapper.readTree(result.toFile());
        assertEquals(1, manifest.path("schemaVersion").asInt());
        assertEquals("0.1.0", manifest.path("applicationVersion").asText());
        assertEquals("totvs-winthor/14-faturamento/" + tempDir.getFileName(),
                manifest.path("collectionId").asText());
        assertEquals("documents.jsonl", manifest.path("artifacts").get(0).path("path").asText());
        assertEquals(Files.size(artifact), manifest.path("artifacts").get(0).path("sizeBytes").asLong());
        assertTrue(manifest.path("artifacts").get(0).path("sha256").asText().matches("[0-9a-f]{64}"));
        assertEquals("pt-br", manifest.path("parameters").path("locale").asText());
        assertTrue(Files.notExists(tempDir.resolve("manifest.json.tmp")));
    }
}
