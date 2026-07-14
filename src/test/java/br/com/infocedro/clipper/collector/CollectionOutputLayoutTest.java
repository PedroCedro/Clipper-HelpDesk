package br.com.infocedro.clipper.collector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CollectionOutputLayoutTest {

    @TempDir
    Path tempDir;

    @Test
    void separaFonteEscopoEExecucaoComNomeSeguroParaWindows() throws Exception {
        OffsetDateTime startedAt = OffsetDateTime.parse("2026-07-14T14:30:52.123-03:00");

        Path result = new CollectionOutputLayout().createExecutionDirectory(
                tempDir, "totvs-winthor", "14-faturamento", startedAt);

        assertEquals(
                tempDir.resolve("totvs-winthor/14-faturamento/20260714-143052-123"),
                result);
        assertTrue(result.toFile().isDirectory());
    }

    @Test
    void colisaoRecebeSufixoSemReutilizarDiretorio() throws Exception {
        OffsetDateTime startedAt = OffsetDateTime.parse("2026-07-14T14:30:52.123-03:00");
        CollectionOutputLayout layout = new CollectionOutputLayout();

        Path first = layout.createExecutionDirectory(tempDir, "manual", "geral", startedAt);
        Path second = layout.createExecutionDirectory(tempDir, "manual", "geral", startedAt);

        assertNotEquals(first, second);
        assertTrue(second.getFileName().toString().matches("20260714-143052-123-[0-9a-f]{8}"));
    }

    @Test
    void rejeitaPathTraversalEmFonteOuEscopo() {
        CollectionOutputLayout layout = new CollectionOutputLayout();
        OffsetDateTime startedAt = OffsetDateTime.now();

        assertThrows(IllegalArgumentException.class,
                () -> layout.createExecutionDirectory(tempDir, "../fora", "geral", startedAt));
        assertThrows(IllegalArgumentException.class,
                () -> layout.createExecutionDirectory(tempDir, "manual", "..", startedAt));
        assertThrows(IllegalArgumentException.class,
                () -> layout.createExecutionDirectory(tempDir, "manual", "pasta\\fora", startedAt));
    }
}
