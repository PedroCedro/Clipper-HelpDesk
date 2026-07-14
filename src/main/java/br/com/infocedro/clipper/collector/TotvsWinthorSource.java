package br.com.infocedro.clipper.collector;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Component;

/** Adaptador da fonte TOTVS/WinThor para o caso de uso genérico de coleta. */
@Component
public class TotvsWinthorSource implements KnowledgeSource {

    public static final String TYPE = "totvs-winthor";

    private final TotvsWinthorCollectorProperties properties;
    private final TotvsWinthorCrawler crawler;

    public TotvsWinthorSource(TotvsWinthorCollectorProperties properties, TotvsWinthorCrawler crawler) {
        this.properties = properties;
        this.crawler = crawler;
    }

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public CollectionSummary collect() throws Exception {
        TotvsWinthorCrawler.CrawlSummary result = crawler.crawl(properties);
        return new CollectionSummary(
                TYPE,
                result.sections(),
                result.articles(),
                result.articlesFile(),
                OffsetDateTime.parse(result.collectedAt()));
    }
}
