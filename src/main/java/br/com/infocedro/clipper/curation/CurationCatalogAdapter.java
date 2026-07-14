package br.com.infocedro.clipper.curation;

import br.com.infocedro.clipper.catalog.RawKnowledgeSearchService;
import org.springframework.stereotype.Component;

/** Confina a dependência do catálogo ao adapter; o caso de uso conhece apenas a porta. */
@Component
class CurationCatalogAdapter implements CurationCatalogPort {

    private final RawKnowledgeSearchService catalogService;

    CurationCatalogAdapter(RawKnowledgeSearchService catalogService) {
        this.catalogService = catalogService;
    }

    @Override
    public boolean exists(Long documentId) {
        return catalogService.exists(documentId);
    }
}
