package br.com.infocedro.clipper.collector;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class TotvsWinthorDocumentMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TotvsWinthorDocumentMapper mapper =
            new TotvsWinthorDocumentMapper(new HtmlTextExtractor());

    @Test
    void ignoraLabelNaoTextualSemPerderLabelsValidas() throws Exception {
        JsonNode payload = objectMapper.readTree("""
                {
                  "id": 42,
                  "title": "Artigo",
                  "body": "Conteúdo",
                  "label_names": ["winthor", 123, {"inesperada": true}, "fiscal"]
                }
                """);
        TotvsWinthorCrawler.SectionCursor section =
                new TotvsWinthorCrawler.SectionCursor(1, "Seção", 2, null, null, "Seção");

        TotvsArticleRecord result = mapper.article(payload, section, "api");

        assertEquals(List.of("winthor", "fiscal"), result.labels());
    }

    @Test
    void preservaContratoJsonlDoArtigoByteAByte() throws Exception {
        TotvsArticleRecord record = new TotvsArticleRecord(
                42L,
                "https://fonte/artigos/42",
                "Título",
                7L,
                "Seção",
                "Produto > Seção",
                List.of("fiscal", "erro_123"),
                "Conteúdo em texto",
                "<p>Conteúdo em texto</p>",
                "2026-01-01T10:00:00Z",
                "2026-01-02T10:00:00Z",
                "https://fonte/api/artigos",
                "2026-07-14T10:00:00-03:00");

        String json = objectMapper.writeValueAsString(record);

        // Golden master: além dos valores, trava nomes e ordem dos campos do
        // JSONL legado. Alterações intencionais exigem revisar este contrato.
        assertEquals("""
                {"id":42,"url":"https://fonte/artigos/42","titulo":"Título","secao_id":7,"secao_nome":"Seção","secao_caminho":"Produto > Seção","labels":["fiscal","erro_123"],"conteudo_texto":"Conteúdo em texto","conteudo_html":"<p>Conteúdo em texto</p>","criado_em":"2026-01-01T10:00:00Z","atualizado_em":"2026-01-02T10:00:00Z","source_api":"https://fonte/api/artigos","collected_at":"2026-07-14T10:00:00-03:00"}""",
                json);
    }

    @Test
    void preservaContratoJsonlDaSecaoByteAByte() throws Exception {
        TotvsSectionRecord record = new TotvsSectionRecord(
                7L,
                "Seção Fiscal",
                "https://fonte/secoes/7",
                3L,
                2L,
                "Produto > Seção Fiscal",
                "2026-07-14T10:00:00-03:00");

        String json = objectMapper.writeValueAsString(record);

        assertEquals("""
                {"id":7,"nome":"Seção Fiscal","url":"https://fonte/secoes/7","category_id":3,"parent_section_id":2,"caminho":"Produto > Seção Fiscal","collected_at":"2026-07-14T10:00:00-03:00"}""",
                json);
    }
}
