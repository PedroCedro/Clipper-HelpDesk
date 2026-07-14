package br.com.infocedro.clipper.collector;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

// Runner de coleta sob demanda. Quando habilitado por configuração, executa o
// crawler uma vez e encerra o contexto para não manter a API web rodando à toa.
@Component
public class TotvsWinthorCrawlerRunner implements ApplicationRunner {

    private final TotvsWinthorCollectorProperties properties;
    private final KnowledgeCrawler crawler;
    private final ConfigurableApplicationContext context;

    public TotvsWinthorCrawlerRunner(
            TotvsWinthorCollectorProperties properties,
            KnowledgeCrawler crawler,
            ConfigurableApplicationContext context
    ) {
        this.properties = properties;
        this.crawler = crawler;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!properties.isEnabled()) {
            return;
        }

        CollectionSummary summary = crawler.crawl(TotvsWinthorSource.TYPE);
        System.out.printf(
                "Coleta %s concluída: %d seções, %d artigos. Saída: %s%n",
                summary.sourceType(),
                summary.containers(),
                summary.documents(),
                summary.outputLocation()
        );
        context.close();
    }
}
