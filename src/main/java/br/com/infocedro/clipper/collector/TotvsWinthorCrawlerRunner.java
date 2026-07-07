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
    private final TotvsWinthorCrawler crawler;
    private final ConfigurableApplicationContext context;

    public TotvsWinthorCrawlerRunner(
            TotvsWinthorCollectorProperties properties,
            TotvsWinthorCrawler crawler,
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

        TotvsWinthorCrawler.CrawlSummary summary = crawler.crawl(properties);
        System.out.printf(
                "TOTVS Winthor crawl concluido: %d secoes, %d artigos. Arquivos: %s e %s%n",
                summary.sections(),
                summary.articles(),
                summary.sectionsFile(),
                summary.articlesFile()
        );
        context.close();
    }
}
