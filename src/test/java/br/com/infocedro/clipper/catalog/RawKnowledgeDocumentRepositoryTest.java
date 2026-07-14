package br.com.infocedro.clipper.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
class RawKnowledgeDocumentRepositoryTest {

    @Autowired
    private RawKnowledgeDocumentRepository repository;

    @Test
    void localizaDocumentoPelaIdentidadeExterna() {
        RawDocumentCandidate candidate = new RawDocumentCandidate(
                "manual", "abc", "geral", null, "Título", "Texto", "<p>Texto</p>", null,
                "", "", "", "{}", null, null, OffsetDateTime.now());
        repository.save(RawKnowledgeDocument.create(candidate, "a".repeat(64), OffsetDateTime.now()));

        List<RawKnowledgeDocument> result = repository.findBySourceTypeAndExternalIdIn(
                "manual", List.of("abc", "outro"));

        assertEquals(1, result.size());
        assertEquals("abc", result.getFirst().getExternalId());
    }
}
