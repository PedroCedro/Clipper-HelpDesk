package br.com.infocedro.clipper.curation;

import java.util.Collection;
import java.util.List;

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

    @Override
    public List<CurationCatalogDocument> findSummaries(Collection<Long> documentIds) {
        return catalogService.findSummaries(documentIds).stream()
                .map(detail -> new CurationCatalogDocument(
                        detail.id(), detail.title(), detail.module(), detail.sourceLabel(), detail.sourceUrl()))
                .toList();
    }
}
