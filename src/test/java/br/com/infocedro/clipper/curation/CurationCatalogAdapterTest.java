package br.com.infocedro.clipper.curation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.infocedro.clipper.catalog.RawDocumentDetail;
import br.com.infocedro.clipper.catalog.RawDocumentNotFoundException;
import br.com.infocedro.clipper.catalog.RawKnowledgeSearchService;
import org.junit.jupiter.api.Test;

class CurationCatalogAdapterTest {

    private final RawKnowledgeSearchService catalogService = mock(RawKnowledgeSearchService.class);
    private final CurationCatalogAdapter adapter = new CurationCatalogAdapter(catalogService);

    @Test
    void confirmaDocumentoPelaApiPublicaDoCatalogo() {
        when(catalogService.findDetail(10L)).thenReturn(mock(RawDocumentDetail.class));

        assertThat(adapter.exists(10L)).isTrue();
    }

    @Test
    void traduzAusenciaDoCatalogoSemVazarExcecao() {
        when(catalogService.findDetail(404L)).thenThrow(new RawDocumentNotFoundException(404L));

        assertThat(adapter.exists(404L)).isFalse();
    }
}
