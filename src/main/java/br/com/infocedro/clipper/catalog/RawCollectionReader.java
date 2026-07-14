package br.com.infocedro.clipper.catalog;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.stereotype.Component;

/** Valida o manifesto antes que qualquer documento alcance o banco. */
@Component
public class RawCollectionReader {

    private final ObjectMapper objectMapper;

    public RawCollectionReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ValidatedRawCollection read(Path executionDirectory) throws IOException {
        Path manifestFile = executionDirectory.resolve("manifest.json");
        ManifestSnapshot manifest = objectMapper.readValue(manifestFile.toFile(), ManifestSnapshot.class);
        Path documentsFile = null;

        for (ArtifactSnapshot artifact : manifest.artifacts()) {
            Path artifactFile = safeArtifact(executionDirectory, artifact.path());
            if (!Files.isRegularFile(artifactFile) || Files.size(artifactFile) != artifact.sizeBytes()
                    || !sha256(artifactFile).equalsIgnoreCase(artifact.sha256())) {
                throw new IllegalArgumentException("Artefato inválido no manifesto: " + artifact.path());
            }
            if (artifact.path().equals("documents.jsonl")) {
                documentsFile = artifactFile;
            }
        }
        if (documentsFile == null) {
            throw new IllegalArgumentException("Manifesto não contém documents.jsonl");
        }
        return new ValidatedRawCollection(
                manifest.collectionId(), manifest.schemaVersion(), manifest.sourceType(),
                manifest.scope(), documentsFile);
    }

    private Path safeArtifact(Path directory, String relativePath) {
        Path resolved = directory.resolve(relativePath).normalize();
        if (Path.of(relativePath).isAbsolute() || !resolved.startsWith(directory.normalize())) {
            throw new IllegalArgumentException("Caminho inseguro no manifesto: " + relativePath);
        }
        return resolved;
    }

    private String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) >= 0) {
                    digest.update(buffer, 0, read);
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", exception);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ManifestSnapshot(
            int schemaVersion,
            String collectionId,
            String sourceType,
            String scope,
            List<ArtifactSnapshot> artifacts
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ArtifactSnapshot(String path, long sizeBytes, String sha256) {
    }
}
