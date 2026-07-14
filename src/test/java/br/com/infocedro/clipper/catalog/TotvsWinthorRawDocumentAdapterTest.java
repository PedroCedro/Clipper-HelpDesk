package br.com.infocedro.clipper.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class TotvsWinthorRawDocumentAdapterTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TotvsWinthorRawDocumentAdapter adapter = new TotvsWinthorRawDocumentAdapter(
            objectMapper, new CatalogHtmlTextExtractor(), new DomainIdentifiersExtractor());

    @Test
    void derivaTextoEMetadadosPesquisaveisDoSnapshot() throws Exception {
        JsonNode document = objectMapper.readTree("""
                {"id":42,"titulo":"Rejeição 1026 na rotina 1452","url":"https://fonte/42",
                 "labels":["FISCAL","rotina_1452"],"conteudo_texto":"texto antigo &eacute;",
                 "conteudo_html":"<p>Solução atualizada &eacute; esta.</p>",
                 "secao_id":7,"secao_nome":"Fiscal","secao_caminho":"Produto > Fiscal",
                 "criado_em":"2026-01-01T10:00:00Z","atualizado_em":"2026-01-02T10:00:00Z",
                 "collected_at":"2026-07-14T10:00:00-03:00"}
                """);

        RawDocumentCandidate result = adapter.adapt(
                document, new RawImportContext("totvs-winthor", "14-faturamento", 1));

        assertEquals("42", result.externalId());
        assertEquals("Solução atualizada é esta.", result.textContent());
        assertEquals("|fiscal|rotina_1452|", result.labelsText());
        assertEquals("|1452|", result.routinesText());
        assertEquals("|1026|", result.errorCodesText());
    }

    @Test
    void aceitaCorpoVazioEIgnoraDataMalformada() throws Exception {
        JsonNode document = objectMapper.readTree("""
                {"id":43,"titulo":"Rascunho","labels":["", "fiscal"],
                 "conteudo_html":"","criado_em":"data-inválida"}
                """);

        RawDocumentCandidate result = adapter.adapt(
                document, new RawImportContext("totvs-winthor", "14-faturamento", 1));

        assertEquals("", result.textContent());
        assertEquals("", result.htmlContent());
        assertEquals("|fiscal|", result.labelsText());
        assertNull(result.sourceCreatedAt());
    }
}
