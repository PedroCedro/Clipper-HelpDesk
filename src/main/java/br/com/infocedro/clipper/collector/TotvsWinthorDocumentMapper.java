package br.com.infocedro.clipper.collector;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Traduz o JSON específico da TOTVS para registros internos tipados. */
@Component
public class TotvsWinthorDocumentMapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(TotvsWinthorDocumentMapper.class);

    private final HtmlTextExtractor textExtractor;

    public TotvsWinthorDocumentMapper(HtmlTextExtractor textExtractor) {
        this.textExtractor = textExtractor;
    }

    public TotvsSectionRecord section(TotvsWinthorCrawler.SectionCursor section) {
        return new TotvsSectionRecord(
                section.id(), section.name(), section.htmlUrl(), section.categoryId(),
                section.parentId(), section.path(), OffsetDateTime.now().toString());
    }

    public TotvsArticleRecord article(JsonNode article, TotvsWinthorCrawler.SectionCursor section, String sourceApi) {
        String html = textOrDefault(article.path("body"), "");
        List<String> labels = labelsOf(article.path("label_names"), article.path("id").asLong());

        return new TotvsArticleRecord(
                article.path("id").asLong(),
                textOrNull(article.path("html_url")),
                textOrDefault(article.path("title"), ""),
                section.id(), section.name(), section.path(), labels,
                textExtractor.extract(html), html,
                textOrNull(article.path("created_at")),
                textOrNull(article.path("updated_at")),
                sourceApi, OffsetDateTime.now().toString());
    }

    private List<String> labelsOf(JsonNode labelsNode, long articleId) {
        if (!labelsNode.isArray()) {
            return List.of();
        }

        List<String> labels = new ArrayList<>();
        for (JsonNode label : labelsNode) {
            if (label.isTextual()) {
                labels.add(label.asText());
            } else {
                // Labels são metadados auxiliares: um item inesperado não
                // pode derrubar todo o lote nem contaminar o modelo tipado.
                LOGGER.warn("Label não textual ignorada no artigo TOTVS {}: {}", articleId, label);
            }
        }
        return List.copyOf(labels);
    }

    private String textOrDefault(JsonNode node, String defaultValue) {
        String value = textOrNull(node);
        return value == null ? defaultValue : value;
    }

    private String textOrNull(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asText();
    }
}
