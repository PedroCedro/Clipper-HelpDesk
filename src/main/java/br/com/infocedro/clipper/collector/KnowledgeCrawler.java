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
    private final CollectionManifestWriter manifestWriter;

    public KnowledgeCrawler(List<KnowledgeSource> sources, CollectionManifestWriter manifestWriter) {
        this.sources = List.copyOf(sources);
        this.manifestWriter = manifestWriter;
    }

    public CollectionSummary crawl(String sourceType) throws Exception {
        CollectionSummary summary = sources.stream()
                .filter(source -> source.type().equalsIgnoreCase(sourceType))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Fonte de conhecimento não registrada: " + sourceType))
                .collect();
        manifestWriter.write(summary);
        return summary;
    }
}
