package br.com.infocedro.clipper.collector;

import java.util.List;

import org.springframework.stereotype.Service;

/**
 * Caso de uso genérico de coleta. Novas integrações entram como beans de
 * {@link KnowledgeSource}; nenhuma condição específica de fornecedor deve
 * ser adicionada aqui.
 */
@Service
public class KnowledgeCrawler {

    private final List<KnowledgeSource> sources;

    public KnowledgeCrawler(List<KnowledgeSource> sources) {
        this.sources = List.copyOf(sources);
    }

    public CollectionSummary crawl(String sourceType) throws Exception {
        return sources.stream()
                .filter(source -> source.type().equalsIgnoreCase(sourceType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Fonte de conhecimento não registrada: " + sourceType))
                .collect();
    }
}
