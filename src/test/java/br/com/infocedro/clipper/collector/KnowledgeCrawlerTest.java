package br.com.infocedro.clipper.collector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class KnowledgeCrawlerTest {

    @Test
    void selecionaFontePorTipoEGeraManifesto() throws Exception {
        OffsetDateTime now = OffsetDateTime.now();
        CollectionSummary expected = new CollectionSummary(
                "manual", "geral", 1, 2, 0, 0, Path.of("memoria"),
                now, now, Map.of(), List.of());
        KnowledgeSource source = new KnowledgeSource() {
            @Override
            public String type() {
                return "manual";
            }

            @Override
            public CollectionSummary collect() {
                return expected;
            }
        };
        CollectionManifestWriter manifestWriter = mock(CollectionManifestWriter.class);

        CollectionSummary result = new KnowledgeCrawler(List.of(source), manifestWriter).crawl("MANUAL");

        assertEquals(expected, result);
        verify(manifestWriter).write(expected);
    }

    @Test
    void rejeitaTipoNaoRegistradoComMensagemClara() {
        KnowledgeCrawler crawler = new KnowledgeCrawler(List.of(), mock(CollectionManifestWriter.class));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> crawler.crawl("desconhecida"));

        assertEquals("Fonte de conhecimento não registrada: desconhecida", error.getMessage());
    }
}
