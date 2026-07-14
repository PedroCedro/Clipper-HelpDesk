package br.com.infocedro.clipper.collector;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/** Gera o manifesto genérico depois que a fonte conclui seus artefatos. */
@Component
public class CollectionManifestWriter {

    private static final int SCHEMA_VERSION = 1;

    private final ObjectMapper objectMapper;
    private final ApplicationVersion applicationVersion;

    public CollectionManifestWriter(ObjectMapper objectMapper, ApplicationVersion applicationVersion) {
        this.objectMapper = objectMapper;
        this.applicationVersion = applicationVersion;
    }

    public Path write(CollectionSummary summary) throws IOException {
        List<CollectionManifest.Artifact> artifacts = new ArrayList<>();
        for (Path artifact : summary.artifacts()) {
            artifacts.add(new CollectionManifest.Artifact(
                    normalizedRelativePath(summary.outputDirectory(), artifact),
                    Files.size(artifact),
                    sha256(artifact)));
        }

        String collectionId = summary.sourceType() + "/" + summary.scope() + "/"
                + summary.outputDirectory().getFileName();
        CollectionManifest manifest = new CollectionManifest(
                SCHEMA_VERSION,
                applicationVersion.value(),
                collectionId,
                summary.sourceType(),
                summary.scope(),
                summary.startedAt(),
                summary.finishedAt(),
                summary.containers(),
                summary.documents(),
                summary.discarded(),
                summary.errors(),
                summary.parameters(),
                List.copyOf(artifacts));

        Path manifestFile = summary.outputDirectory().resolve("manifest.json");
        publishAtomically(manifestFile, manifest);
        return manifestFile;
    }

    private void publishAtomically(Path manifestFile, CollectionManifest manifest) throws IOException {
        Path temporaryFile = manifestFile.resolveSibling("manifest.json.tmp");
        byte[] content = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(manifest);

        Files.deleteIfExists(temporaryFile);
        try {
            // force(true) reduz a janela em que o rename existe, mas o conteúdo
            // ainda está apenas no cache do sistema operacional.
            try (FileChannel channel = FileChannel.open(
                    temporaryFile, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(content);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
            try {
                Files.move(temporaryFile, manifestFile,
                        StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                // Mesmo diretório preserva a melhor garantia disponível em
                // sistemas de arquivos que não implementam rename atômico.
                Files.move(temporaryFile, manifestFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporaryFile);
        }
    }

    private String normalizedRelativePath(Path outputDirectory, Path artifact) {
        // O manifesto usa separador estável mesmo quando a coleta roda no Windows.
        return outputDirectory.relativize(artifact).toString().replace('\\', '/');
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
            // SHA-256 faz parte obrigatória da JVM; esta exceção representa
            // ambiente Java inválido, não uma falha recuperável da coleta.
            throw new IllegalStateException("SHA-256 indisponível na JVM", exception);
        }
    }
}
