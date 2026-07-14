package br.com.infocedro.clipper.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RawKnowledgeImportServiceTest {

    @TempDir
    Path tempDir;

    private RawKnowledgeDocumentRepository repository;
    private RawCollectionReader reader;
    private RawDocumentFingerprint fingerprint;
    private ObjectMapper objectMapper;
    private RawKnowledgeImportService service;

    @BeforeEach
    void setUp() throws Exception {
        repository = mock(RawKnowledgeDocumentRepository.class);
        reader = mock(RawCollectionReader.class);
        fingerprint = new RawDocumentFingerprint();
        objectMapper = new ObjectMapper();
        RawDocumentAdapter adapter = new TotvsWinthorRawDocumentAdapter(
                objectMapper, new CatalogHtmlTextExtractor(), new DomainIdentifiersExtractor());
        service = new RawKnowledgeImportService(
                repository, reader, fingerprint, objectMapper, List.of(adapter));

        Path documents = tempDir.resolve("documents.jsonl");
        Files.writeString(documents, jsonLine());
        when(reader.read(tempDir)).thenReturn(new ValidatedRawCollection(
                "totvs-winthor/14-faturamento/execucao", 1,
                "totvs-winthor", "14-faturamento", documents));
    }

    @Test
    void insereDocumentoNovo() throws Exception {
        when(repository.findBySourceTypeAndExternalIdIn(any(), any())).thenReturn(List.of());

        RawImportResult result = service.importCollection(tempDir);

        assertEquals(1, result.inserted());
        assertEquals(0, result.updated());
        assertEquals(0, result.unchanged());
        verify(repository).save(any(RawKnowledgeDocument.class));
    }

    @Test
    void reimportacaoComMesmoHashFicaInalterada() throws Exception {
        JsonNodeCandidate candidate = candidateFromLine();
        RawKnowledgeDocument existing = RawKnowledgeDocument.create(
                candidate.value(), fingerprint.calculate(candidate.value()), OffsetDateTime.now());
        when(repository.findBySourceTypeAndExternalIdIn(any(), any())).thenReturn(List.of(existing));

        RawImportResult result = service.importCollection(tempDir);

        assertEquals(1, result.unchanged());
        verify(repository, never()).save(any(RawKnowledgeDocument.class));
    }

    @Test
    void hashDiferenteAtualizaDocumentoExistente() throws Exception {
        RawDocumentCandidate candidate = candidateFromLine().value();
        RawKnowledgeDocument existing = RawKnowledgeDocument.create(
                candidate, "0".repeat(64), OffsetDateTime.now());
        when(repository.findBySourceTypeAndExternalIdIn(any(), any())).thenReturn(List.of(existing));

        RawImportResult result = service.importCollection(tempDir);

        assertEquals(1, result.updated());
        assertEquals(fingerprint.calculate(candidate), existing.getContentHash());
        verify(repository, never()).save(any(RawKnowledgeDocument.class));
    }

    @Test
    void documentoInvalidoImpedeQualquerPersistenciaDoModulo() throws Exception {
        Path documents = tempDir.resolve("documents.jsonl");
        Files.writeString(documents, jsonLine() + "\n{\"id\":99,\"conteudo_html\":\"\"}\n");

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> service.importCollection(tempDir));

        // O serviço materializa e valida todos os candidatos antes do primeiro
        // save; dado inválido não deixa importação parcial no catálogo.
        verifyNoInteractions(repository);
    }

    private JsonNodeCandidate candidateFromLine() throws Exception {
        RawDocumentAdapter adapter = new TotvsWinthorRawDocumentAdapter(
                objectMapper, new CatalogHtmlTextExtractor(), new DomainIdentifiersExtractor());
        return new JsonNodeCandidate(adapter.adapt(
                objectMapper.readTree(jsonLine()),
                new RawImportContext("totvs-winthor", "14-faturamento", 1)));
    }

    private String jsonLine() {
        return """
                {"id":42,"titulo":"Rotina 1452","labels":["fiscal"],
                 "conteudo_html":"<p>Texto</p>","secao_id":7,
                 "criado_em":"2026-01-01T10:00:00Z","atualizado_em":"2026-01-02T10:00:00Z",
                 "collected_at":"2026-07-14T10:00:00-03:00"}
                """.replaceAll("\\R\\s*", "");
    }

    private record JsonNodeCandidate(RawDocumentCandidate value) {
    }
}
