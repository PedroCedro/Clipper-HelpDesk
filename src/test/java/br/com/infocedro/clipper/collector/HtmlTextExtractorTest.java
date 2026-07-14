package br.com.infocedro.clipper.collector;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class HtmlTextExtractorTest {

    private final HtmlTextExtractor extractor = new HtmlTextExtractor();

    @Test
    void preservaParagrafosListasEEntidades() {
        String html = "<p>Dúvida &amp; causa</p><ul><li>Passo&#32;1</li><li>Passo 2</li></ul>";

        assertEquals("Dúvida & causa\nPasso 1\nPasso 2", extractor.extract(html));
    }

    @Test
    void conteudoAusenteViraTextoVazio() {
        assertEquals("", extractor.extract(null));
        assertEquals("", extractor.extract("   "));
    }

    @Test
    void decodificaEntidadesNomeadasComuns() {
        assertEquals("Solução, rejeição é condição nº 1", extractor.extract(
                "Solu&ccedil;&atilde;o, rejei&ccedil;&atilde;o &eacute; condi&ccedil;&atilde;o n&ordm; 1"));
    }

    @Test
    void naoFazDuplaDecodificacao() {
        assertEquals("&lt;campo&gt;", extractor.extract("&amp;lt;campo&amp;gt;"));
    }

    @Test
    void entidadeNumericaMalformadaNaoInterrompeExtracao() {
        assertEquals("valor &#fe; inválido", extractor.extract("valor &#fe; inv&aacute;lido"));
    }
}
