package br.com.infocedro.clipper.collector;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/** Reserva o endereço exclusivo de uma execução sem conhecer fornecedores. */
@Component
public class CollectionOutputLayout {

    private static final DateTimeFormatter EXECUTION_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
    private static final Pattern SAFE_SEGMENT = Pattern.compile("[a-z0-9][a-z0-9._-]*");

    public Path createExecutionDirectory(
            Path rawRoot,
            String sourceType,
            String scope,
            OffsetDateTime startedAt
    ) throws IOException {
        validateSegment("tipo da fonte", sourceType);
        validateSegment("escopo", scope);

        Path scopeDirectory = rawRoot.resolve(sourceType).resolve(scope);
        Files.createDirectories(scopeDirectory);
        String baseName = EXECUTION_FORMAT.format(startedAt);

        try {
            return Files.createDirectory(scopeDirectory.resolve(baseName));
        } catch (FileAlreadyExistsException collision) {
            // A reserva exclusiva impede duas coletas concorrentes de abrirem
            // os mesmos JSONL. O sufixo mantém o timestamp legível e único.
            String suffix = UUID.randomUUID().toString().substring(0, 8);
            return Files.createDirectory(scopeDirectory.resolve(baseName + "-" + suffix));
        }
    }

    private void validateSegment(String field, String value) {
        if (value == null || !SAFE_SEGMENT.matcher(value).matches()
                || value.equals(".") || value.equals("..")) {
            throw new IllegalArgumentException(
                    "Segmento inválido para " + field + ": " + value);
        }
    }
}
