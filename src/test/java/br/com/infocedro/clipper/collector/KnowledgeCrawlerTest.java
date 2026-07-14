package br.com.infocedro.clipper.collector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

class KnowledgeCrawlerTest {

    @Test
    void selecionaFontePorTipoSemConhecerFornecedor() throws Exception {
        KnowledgeSource source = new KnowledgeSource() {
            @Override
            public String type() {
                return "manual";
            }

            @Override
            public CollectionSummary collect() {
                return new CollectionSummary(type(), 1, 2, "memoria", OffsetDateTime.now());
            }
        };

        CollectionSummary result = new KnowledgeCrawler(List.of(source)).crawl("MANUAL");

        assertEquals("manual", result.sourceType());
        assertEquals(2, result.documents());
    }

    @Test
    void rejeitaTipoNaoRegistradoComMensagemClara() {
        KnowledgeCrawler crawler = new KnowledgeCrawler(List.of());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> crawler.crawl("desconhecida"));

        assertEquals("Fonte de conhecimento não registrada: desconhecida", error.getMessage());
    }
}
