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

    @Test
    void pesquisaComProjecaoSemCarregarSnapshotHtml() {
        RawDocumentCandidate candidate = new RawDocumentCandidate(
                "totvs-winthor", "42", "14-faturamento", "14-faturamento",
                "Rejeição 1026", "Falha fiscal", "<p>HTML grande</p>", "https://fonte/42",
                "|fiscal|", "", "|1026|", "{}", null,
                OffsetDateTime.parse("2026-01-02T10:00:00Z"), OffsetDateTime.now());
        repository.saveAndFlush(RawKnowledgeDocument.create(candidate, "b".repeat(64), OffsetDateTime.now()));

        List<RawSearchCandidate> result = repository.searchCandidates(
                "1026", "totvs-winthor", "14-faturamento");

        assertEquals(1, result.size());
        assertEquals("42", result.getFirst().getExternalId());
        assertEquals("|1026|", result.getFirst().getErrorCodesText());
    }
}
