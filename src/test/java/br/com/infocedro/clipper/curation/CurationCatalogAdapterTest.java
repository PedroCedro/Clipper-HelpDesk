package br.com.infocedro.clipper.curation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.infocedro.clipper.catalog.RawKnowledgeSearchService;
import br.com.infocedro.clipper.catalog.RawDocumentDetail;
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
    void traduzDetalheParaContratoNeutroDaCuradoria() {
        when(catalogService.findDetail(10L)).thenReturn(new RawDocumentDetail(
                10L, "ext-10", "Fonte oficial", "fiscal", "14-faturamento", "Rotina fiscal",
                "Texto", "https://fonte/10", "", "", "", null, null, null));

        CurationCatalogDocument document = adapter.find(10L);

        assertThat(document.title()).isEqualTo("Rotina fiscal");
        assertThat(document.sourceLabel()).isEqualTo("Fonte oficial");
    }
}
