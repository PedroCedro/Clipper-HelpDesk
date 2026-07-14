package br.com.infocedro.clipper.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RawKnowledgeSearchServiceTest {

    private RawKnowledgeDocumentRepository repository;
    private RawKnowledgeSearchService service;

    @BeforeEach
    void setUp() {
        repository = mock(RawKnowledgeDocumentRepository.class);
        service = new RawKnowledgeSearchService(repository);
    }

    @Test
    void verificaExistenciaSemCarregarDetalhe() {
        when(repository.existsById(7L)).thenReturn(true);

        assertTrue(service.exists(7L));
        assertFalse(service.exists(null));
    }

    @Test
    void buscaResumosEmLoteSemCarregarConteudo() {
        RawDocumentSummaryProjection projection = mock(RawDocumentSummaryProjection.class);
        when(projection.getId()).thenReturn(7L);
        when(projection.getSourceType()).thenReturn("totvs-winthor");
        when(projection.getTitle()).thenReturn("Rotina fiscal");
        when(repository.findSummariesByIdIn(Set.of(7L))).thenReturn(List.of(projection));

        List<RawDocumentSummary> summaries = service.findSummaries(Set.of(7L));

        assertEquals(1, summaries.size());
        assertEquals("Fonte oficial", summaries.getFirst().sourceLabel());
    }

    @Test
    void identificadorExatoRecebeMaiorPesoEExplicaMatch() {
        RawSearchCandidate exact = candidate(
                1L, "Rejeição 1026 na nota", "A rejeição ocorre", "|fiscal|", "", "|1026|");
        RawSearchCandidate textual = candidate(
                2L, "Dúvida fiscal", "Texto menciona rejeição 1026", "|fiscal|", "", "");
        when(repository.searchCandidates(any(), any(), any())).thenReturn(List.of(textual, exact));

        RawSearchPage result = service.search(new RawSearchQuery("1026", null, null, 0, 20));

        assertEquals(2, result.total());
        assertEquals(1L, result.items().getFirst().id());
        assertTrue(result.items().getFirst().matchReasons().contains(RawMatchReason.EXACT_IDENTIFIER));
        assertTrue(result.items().getFirst().score() > result.items().get(1).score());
    }

    @Test
    void numeroNaoCasaPorPrefixo() {
        RawSearchCandidate routine1443 = candidate(
                1L, "Como usar a rotina 1443", "Procedimento da rotina 1443", "", "|1443|", "");
        when(repository.searchCandidates(any(), any(), any())).thenReturn(List.of(routine1443));

        RawSearchPage result = service.search(new RawSearchQuery("144", null, null, 0, 20));

        assertEquals(0, result.total());
        assertTrue(result.items().isEmpty());
    }

    @Test
    void paginaDepoisDeOrdenarTodosOsCandidatos() {
        List<RawSearchCandidate> candidates = List.of(
                candidate(1L, "Fiscal", "fiscal", "", "", ""),
                candidate(2L, "Fiscal dois", "fiscal", "", "", ""),
                candidate(3L, "Fiscal três", "fiscal", "", "", ""));
        when(repository.searchCandidates(any(), any(), any())).thenReturn(candidates);

        RawSearchPage result = service.search(new RawSearchQuery("fiscal", null, null, 1, 2));

        assertEquals(3, result.total());
        assertEquals(1, result.items().size());
        assertEquals(1, result.page());
    }

    @Test
    void validaTextoPaginaETamanho() {
        assertThrows(IllegalArgumentException.class,
                () -> service.search(new RawSearchQuery(" ", null, null, 0, 20)));
        assertThrows(IllegalArgumentException.class,
                () -> service.search(new RawSearchQuery("ok", null, null, -1, 20)));
        assertThrows(IllegalArgumentException.class,
                () -> service.search(new RawSearchQuery("ok", null, null, 0, 51)));
    }

    private RawSearchCandidate candidate(
            Long id, String title, String content, String labels, String routines, String errors) {
        RawSearchCandidate candidate = mock(RawSearchCandidate.class);
        when(candidate.getId()).thenReturn(id);
        when(candidate.getExternalId()).thenReturn(String.valueOf(id));
        when(candidate.getTitle()).thenReturn(title);
        when(candidate.getTextContent()).thenReturn(content);
        when(candidate.getLabelsText()).thenReturn(labels);
        when(candidate.getRoutinesText()).thenReturn(routines);
        when(candidate.getErrorCodesText()).thenReturn(errors);
        when(candidate.getModule()).thenReturn("14-faturamento");
        when(candidate.getSourceUrl()).thenReturn("https://fonte/" + id);
        when(candidate.getSourceUpdatedAt()).thenReturn(OffsetDateTime.parse("2026-01-01T10:00:00Z"));
        return candidate;
    }
}
