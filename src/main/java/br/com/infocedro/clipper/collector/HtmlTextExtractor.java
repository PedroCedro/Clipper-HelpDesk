package br.com.infocedro.clipper.collector;

import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/**
 * Converte o HTML recebido das fontes externas em texto adequado para busca e
 * curadoria. A classe não conhece TOTVS nem persistência, portanto pode ser
 * reutilizada por futuros adaptadores que entreguem conteúdo HTML.
 */
@Component
public class HtmlTextExtractor {

    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>");

    public String extract(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }

        // Preserva limites semânticos antes de remover as tags. Sem isso,
        // parágrafos e itens de lista diferentes seriam concatenados.
        String text = html
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n")
                .replaceAll("(?i)</li>", "\n")
                .replaceAll("(?i)</h[1-6]>", "\n")
                .replaceAll("(?i)</tr>", "\n");

        text = TAG_PATTERN.matcher(text).replaceAll(" ");
        // A implementação do Spring cobre entidades HTML nomeadas e mantém
        // entradas inválidas literais, sem arriscar interromper a coleta.
        text = HtmlUtils.htmlUnescape(text);
        return text.replace('\u00A0', ' ')
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

}
