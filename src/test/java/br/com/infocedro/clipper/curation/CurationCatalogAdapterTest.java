package br.com.infocedro.clipper.curation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.infocedro.clipper.catalog.RawKnowledgeSearchService;
import br.com.infocedro.clipper.catalog.RawDocumentSummary;
import java.util.List;
import org.junit.jupiter.api.Test;

class CurationCatalogAdapterTest {

    private final RawKnowledgeSearchService catalogService = mock(RawKnowledgeSearchService.class);
    private final CurationCatalogAdapter adapter = new CurationCatalogAdapter(catalogService);

    @Test
    void confirmaDocumentoPelaApiPublicaDoCatalogo() {
        when(catalogService.exists(10L)).thenReturn(true);

        assertThat(adapter.exists(10L)).isTrue();
    }

    @Test
    void informaAusenciaPelaApiPublicaDoCatalogo() {
        when(catalogService.exists(404L)).thenReturn(false);

        assertThat(adapter.exists(404L)).isFalse();
    }

    @Test
    void traduzResumoLeveParaContratoNeutroDaCuradoria() {
        when(catalogService.findSummaries(List.of(10L))).thenReturn(List.of(new RawDocumentSummary(
                10L, "Rotina fiscal", "14-faturamento", "Fonte oficial", "https://fonte/10")));

        CurationCatalogDocument document = adapter.findSummaries(List.of(10L)).getFirst();

        assertThat(document.title()).isEqualTo("Rotina fiscal");
        assertThat(document.sourceLabel()).isEqualTo("Fonte oficial");
    }
}
