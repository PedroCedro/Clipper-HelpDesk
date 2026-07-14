package br.com.infocedro.clipper.catalog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.springframework.stereotype.Component;

/** Localiza a execução concluída mais recente para uma fonte e escopo. */
@Component
public class RawCollectionLocator {

    public Path latest(Path rawRoot, String sourceType, String scope) throws IOException {
        Path scopeDirectory = rawRoot.resolve(sourceType).resolve(scope);
        try (var directories = Files.list(scopeDirectory)) {
            return directories
                    .filter(Files::isDirectory)
                    .filter(path -> Files.isRegularFile(path.resolve("manifest.json")))
                    .max(Comparator.comparing(path -> path.getFileName().toString()))
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Nenhuma coleta completa encontrada para " + sourceType + "/" + scope));
        }
    }
}
