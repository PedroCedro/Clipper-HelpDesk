package br.com.infocedro.clipper.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RawCollectionReaderTest {

    @TempDir
    Path tempDir;

    @Test
    void leManifestoVerbatimComCamposExtrasEValidaHash() throws Exception {
        Path documents = tempDir.resolve("documents.jsonl");
        Files.writeString(documents, "{}\n");
        writeManifest(sha256(documents));

        ValidatedRawCollection result = new RawCollectionReader(
                new ObjectMapper().findAndRegisterModules()).read(tempDir);

        assertEquals("totvs-winthor", result.sourceType());
        assertEquals("14-faturamento", result.scope());
        assertEquals(documents, result.documentsFile());
    }

    @Test
    void rejeitaArtefatoComHashDivergente() throws Exception {
        Files.writeString(tempDir.resolve("documents.jsonl"), "{}\n");
        writeManifest("0".repeat(64));

        assertThrows(IllegalArgumentException.class, () -> new RawCollectionReader(
                new ObjectMapper().findAndRegisterModules()).read(tempDir));
    }

    private void writeManifest(String hash) throws Exception {
        String manifest = """
                {"schemaVersion":1,"applicationVersion":"0.1.0","collectionId":"execução-1",
                 "sourceType":"totvs-winthor","scope":"14-faturamento",
                 "startedAt":"2026-07-14T10:00:00-03:00","documents":1,
                 "parameters":{"campoExtra":true},"artifacts":[
                   {"path":"documents.jsonl","sizeBytes":3,"sha256":"%s"}]}
                """.formatted(hash);
        Files.writeString(tempDir.resolve("manifest.json"), manifest);
    }

    private String sha256(Path file) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
    }
}
