package br.com.infocedro.clipper.collector;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

// Runner de coleta sob demanda. O script inicia a aplicação em modo não-web;
// depois dos runners, o processo encerra naturalmente sem fechar o contexto
// enquanto outros ApplicationRunner ainda estão em execução.
@Component
public class TotvsWinthorCrawlerRunner implements ApplicationRunner {

    private final TotvsWinthorCollectorProperties properties;
    private final KnowledgeCrawler crawler;

    public TotvsWinthorCrawlerRunner(
            TotvsWinthorCollectorProperties properties,
            KnowledgeCrawler crawler
    ) {
        this.properties = properties;
        this.crawler = crawler;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!properties.isEnabled()) {
            return;
        }

        CollectionSummary summary = crawler.crawl(TotvsWinthorSource.TYPE);
        System.out.printf(
                "Coleta %s/%s concluída: %d seções, %d artigos. Saída: %s%n",
                summary.sourceType(),
                summary.scope(),
                summary.containers(),
                summary.documents(),
                summary.outputDirectory()
        );
    }
}
