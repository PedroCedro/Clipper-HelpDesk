package br.com.infocedro.clipper.catalog;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Importa uma coleta validada de forma atômica e idempotente. */
@Service
public class RawKnowledgeImportService {

    private final RawKnowledgeDocumentRepository repository;
    private final RawCollectionReader collectionReader;
    private final RawDocumentFingerprint fingerprint;
    private final ObjectMapper objectMapper;
    private final List<RawDocumentAdapter> adapters;

    public RawKnowledgeImportService(
            RawKnowledgeDocumentRepository repository,
            RawCollectionReader collectionReader,
            RawDocumentFingerprint fingerprint,
            ObjectMapper objectMapper,
            List<RawDocumentAdapter> adapters
    ) {
        this.repository = repository;
        this.collectionReader = collectionReader;
        this.fingerprint = fingerprint;
        this.objectMapper = objectMapper;
        this.adapters = List.copyOf(adapters);
    }

    @Transactional(rollbackFor = Exception.class)
    public RawImportResult importCollection(Path executionDirectory) throws IOException {
        ValidatedRawCollection collection = collectionReader.read(executionDirectory);
        RawDocumentAdapter adapter = requireAdapter(collection);
        RawImportContext context = new RawImportContext(
                collection.sourceType(), collection.scope(), collection.schemaVersion());
        Map<String, RawDocumentCandidate> candidates = readCandidates(collection.documentsFile(), adapter, context);

        Map<String, RawKnowledgeDocument> existing = new HashMap<>();
        repository.findBySourceTypeAndExternalIdIn(collection.sourceType(), candidates.keySet())
                .forEach(document -> existing.put(document.getExternalId(), document));

        int inserted = 0;
        int updated = 0;
        int unchanged = 0;
        OffsetDateTime now = OffsetDateTime.now();
        for (RawDocumentCandidate candidate : candidates.values()) {
            String hash = fingerprint.calculate(candidate);
            RawKnowledgeDocument current = existing.get(candidate.externalId());
            if (current == null) {
                repository.save(RawKnowledgeDocument.create(candidate, hash, now));
                inserted++;
            } else if (!current.getContentHash().equals(hash)) {
                current.apply(candidate, hash, now);
                updated++;
            } else {
                unchanged++;
            }
        }
        return new RawImportResult(collection.collectionId(), inserted, updated, unchanged);
    }

    private Map<String, RawDocumentCandidate> readCandidates(
            Path file, RawDocumentAdapter adapter, RawImportContext context) throws IOException {
        Map<String, RawDocumentCandidate> candidates = new LinkedHashMap<>();
        int lineNumber = 0;
        for (String line : Files.readAllLines(file)) {
            lineNumber++;
            if (line.isBlank()) {
                continue;
            }
            JsonNode json = objectMapper.readTree(line);
            RawDocumentCandidate candidate = adapter.adapt(json, context);
            if (candidates.putIfAbsent(candidate.externalId(), candidate) != null) {
                throw new IllegalArgumentException(
                        "Documento duplicado no JSONL, linha " + lineNumber + ": " + candidate.externalId());
            }
        }
        return candidates;
    }

    private RawDocumentAdapter requireAdapter(ValidatedRawCollection collection) {
        return adapters.stream()
                .filter(adapter -> adapter.supports(collection.sourceType(), collection.schemaVersion()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Adapter não registrado para " + collection.sourceType()
                                + " schema " + collection.schemaVersion()));
    }
}
