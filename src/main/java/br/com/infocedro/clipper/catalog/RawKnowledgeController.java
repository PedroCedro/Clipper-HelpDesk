package br.com.infocedro.clipper.catalog;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Borda HTTP somente leitura do catálogo usado pela futura curadoria. */
@RestController
@RequestMapping("/api/raw-documents")
public class RawKnowledgeController {

    private final RawKnowledgeSearchService searchService;

    public RawKnowledgeController(RawKnowledgeSearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping("/search")
    public RawSearchPage search(
            @RequestParam("q") String query,
            @RequestParam(name = "sourceType", required = false) String sourceType,
            @RequestParam(name = "module", required = false) String module,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return searchService.search(new RawSearchQuery(query, sourceType, module, page, size));
    }

    @GetMapping("/{id}")
    public RawDocumentDetail detail(@PathVariable Long id) {
        return searchService.findDetail(id);
    }
}
