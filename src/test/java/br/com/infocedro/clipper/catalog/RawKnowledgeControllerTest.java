package br.com.infocedro.clipper.catalog;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/** Trava o contrato HTTP que será consumido pela interface de curadoria. */
@WebMvcTest(RawKnowledgeController.class)
class RawKnowledgeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RawKnowledgeSearchService searchService;

    @Test
    void buscaRetornaPaginaComRankingEMotivos() throws Exception {
        RawSearchItem item = new RawSearchItem(
                7L, "42", "Rejeição 1026", "14-faturamento", 13,
                Set.of(RawMatchReason.EXACT_IDENTIFIER, RawMatchReason.TITLE),
                "Trecho relevante", "https://fonte/42", OffsetDateTime.parse("2026-01-02T10:00:00Z"));
        when(searchService.search(new RawSearchQuery(
                "1026", "totvs-winthor", "14-faturamento", 0, 20)))
                .thenReturn(new RawSearchPage(List.of(item), 0, 20, 1));

        mockMvc.perform(get("/api/raw-documents/search")
                        .param("q", "1026")
                        .param("sourceType", "totvs-winthor")
                        .param("module", "14-faturamento"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].score").value(13))
                .andExpect(jsonPath("$.items[0].matchReasons").isArray());
    }

    @Test
    void detalheNaoExpoeHtmlNemMetadataInterna() throws Exception {
        RawDocumentDetail detail = new RawDocumentDetail(
                7L, "42", "totvs-winthor", "14-faturamento", "14-faturamento",
                "Título", "Texto seguro", "https://fonte/42", "|fiscal|", "", "|1026|",
                null, null, null);
        when(searchService.findDetail(7L)).thenReturn(detail);

        mockMvc.perform(get("/api/raw-documents/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.textContent").value("Texto seguro"))
                .andExpect(jsonPath("$.htmlContent").doesNotExist())
                .andExpect(jsonPath("$.metadataJson").doesNotExist());
    }

    @Test
    void parametrosInvalidosRetornam400() throws Exception {
        RawSearchQuery query = new RawSearchQuery("x", null, null, 0, 20);
        when(searchService.search(query)).thenThrow(new InvalidRawSearchException("Busca inválida"));

        mockMvc.perform(get("/api/raw-documents/search").param("q", "x"))
                .andExpect(status().isBadRequest());
        verify(searchService).search(query);
    }

    @Test
    void documentoInexistenteRetorna404() throws Exception {
        when(searchService.findDetail(99L)).thenThrow(new RawDocumentNotFoundException(99L));

        mockMvc.perform(get("/api/raw-documents/99"))
                .andExpect(status().isNotFound());
    }
}
