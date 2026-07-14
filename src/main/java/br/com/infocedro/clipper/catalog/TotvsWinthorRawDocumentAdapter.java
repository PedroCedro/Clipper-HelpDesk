package br.com.infocedro.clipper.catalog;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Adapter do JSONL TOTVS/WinThor para o catálogo neutro. */
@Component
public class TotvsWinthorRawDocumentAdapter implements RawDocumentAdapter {

    private static final String SOURCE_TYPE = "totvs-winthor";
    private static final Logger LOGGER = LoggerFactory.getLogger(TotvsWinthorRawDocumentAdapter.class);

    private final ObjectMapper objectMapper;
    private final CatalogHtmlTextExtractor textExtractor;
    private final DomainIdentifiersExtractor identifiers;

    public TotvsWinthorRawDocumentAdapter(
            ObjectMapper objectMapper,
            CatalogHtmlTextExtractor textExtractor,
            DomainIdentifiersExtractor identifiers
    ) {
        this.objectMapper = objectMapper;
        this.textExtractor = textExtractor;
        this.identifiers = identifiers;
    }

    @Override
    public boolean supports(String sourceType, int schemaVersion) {
        return SOURCE_TYPE.equalsIgnoreCase(sourceType) && schemaVersion == 1;
    }

    @Override
    public RawDocumentCandidate adapt(JsonNode document, RawImportContext context) {
        // Corpo vazio é um estado válido em artigos-rascunho da fonte. O
        // catálogo preserva o snapshot em vez de bloquear o módulo inteiro.
        String html = textOrDefault(document.path("conteudo_html"), "");
        String title = requiredText(document, "titulo");
        List<String> labels = textualLabels(document.path("labels"));
        String textContent = textExtractor.extract(html);
        String searchableContext = title + " " + String.join(" ", labels) + " " + textContent;

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("sectionId", document.path("secao_id").asLong());
        metadata.put("sectionName", textOrNull(document.path("secao_nome")));
        metadata.put("sectionPath", textOrNull(document.path("secao_caminho")));
        metadata.put("labels", labels);

        return new RawDocumentCandidate(
                context.sourceType(),
                String.valueOf(document.path("id").asLong()),
                context.scope(),
                context.scope(),
                title,
                textContent,
                html,
                textOrNull(document.path("url")),
                identifiers.normalizedDelimited(labels),
                identifiers.routines(searchableContext),
                identifiers.errorCodes(searchableContext),
                json(metadata),
                dateOrNull(document.path("criado_em")),
                dateOrNull(document.path("atualizado_em")),
                dateOrNull(document.path("collected_at")));
    }

    private List<String> textualLabels(JsonNode node) {
        List<String> labels = new ArrayList<>();
        if (node.isArray()) {
            node.forEach(label -> {
                if (label.isTextual() && !label.asText().isBlank()) {
                    labels.add(label.asText());
                }
            });
        }
        return List.copyOf(labels);
    }

    private String requiredText(JsonNode document, String field) {
        String value = textOrNull(document.path(field));
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Campo obrigatório ausente no documento bruto: " + field);
        }
        return value;
    }

    private String textOrNull(JsonNode node) {
        return node.isMissingNode() || node.isNull() ? null : node.asText();
    }

    private String textOrDefault(JsonNode node, String defaultValue) {
        String value = textOrNull(node);
        return value != null ? value : defaultValue;
    }

    private OffsetDateTime dateOrNull(JsonNode node) {
        String value = textOrNull(node);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value);
        } catch (DateTimeParseException exception) {
            // Data da fonte é metadado auxiliar. Preservar os demais campos é
            // mais útil do que tornar todo o módulo inelegível para curadoria.
            LOGGER.warn("Data inválida ignorada no documento TOTVS: {}", value);
            return null;
        }
    }

    private String json(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Não foi possível serializar metadados do documento", exception);
        }
    }
}
