package br.com.infocedro.clipper.catalog;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * Deriva o texto canônico do catálogo a partir do snapshot HTML. Não reutiliza
 * o extrator do coletor: este é o resultado indexado e pode evoluir por
 * reimportação, enquanto o outro preserva a compatibilidade do JSONL.
 */
@Component
public class CatalogHtmlTextExtractor {

    public String extract(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }
        String text = html
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>|</li>|</h[1-6]>|</tr>", "\n")
                .replaceAll("<[^>]+>", " ");
        return HtmlUtils.htmlUnescape(text).replace('\u00A0', ' ')
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
