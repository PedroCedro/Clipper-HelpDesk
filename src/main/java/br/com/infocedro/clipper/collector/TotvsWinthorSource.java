package br.com.infocedro.clipper.collector;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

/** Adaptador da fonte TOTVS/WinThor para o caso de uso genérico de coleta. */
@Component
public class TotvsWinthorSource implements KnowledgeSource {

    public static final String TYPE = "totvs-winthor";

    private final TotvsWinthorCollectorProperties properties;
    private final TotvsWinthorModuleCatalog moduleCatalog;
    private final CollectionOutputLayout outputLayout;
    private final TotvsWinthorCrawler crawler;

    public TotvsWinthorSource(
            TotvsWinthorCollectorProperties properties,
            TotvsWinthorModuleCatalog moduleCatalog,
            CollectionOutputLayout outputLayout,
            TotvsWinthorCrawler crawler
    ) {
        this.properties = properties;
        this.moduleCatalog = moduleCatalog;
        this.outputLayout = outputLayout;
        this.crawler = crawler;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public CollectionSummary collect() throws Exception {
        if (properties.getModule() == null || properties.getModule().isBlank()) {
            throw new IllegalArgumentException("Informe o módulo com clipper.collector.totvs-winthor.module");
        }

        TotvsWinthorModule module = moduleCatalog.require(properties.getModule());
        OffsetDateTime startedAt = OffsetDateTime.now();
        Path outputDirectory = outputLayout.createExecutionDirectory(
                properties.getRawRoot(), TYPE, module.key(), startedAt);
        TotvsWinthorCrawler.CrawlSummary result = crawler.crawl(
                properties, module, outputDirectory, startedAt);

        return new CollectionSummary(
                TYPE,
                module.key(),
                result.sections(),
                result.articles(),
                0,
                0,
                outputDirectory,
                result.startedAt(),
                result.finishedAt(),
                Map.of(
                        "locale", properties.getLocale(),
                        "rootSectionId", module.rootSectionId(),
                        "rootSectionName", module.name(),
                        "perPage", properties.getPerPage(),
                        "requestDelayMillis", properties.getRequestDelayMillis(),
                        "maxSections", properties.getMaxSections(),
                        "maxArticles", properties.getMaxArticles()),
                List.of(result.sectionsFile(), result.articlesFile()));
    }
}
