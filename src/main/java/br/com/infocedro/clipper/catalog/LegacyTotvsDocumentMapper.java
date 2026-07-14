package br.com.infocedro.clipper.catalog;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

/** Traduz a tabela histórica para o contrato neutro usado pelo catálogo atual. */
@Component
class LegacyTotvsDocumentMapper {

    private final ObjectMapper objectMapper;
    private final DomainIdentifiersExtractor identifiersExtractor;

    LegacyTotvsDocumentMapper(ObjectMapper objectMapper, DomainIdentifiersExtractor identifiersExtractor) {
        this.objectMapper = objectMapper;
        this.identifiersExtractor = identifiersExtractor;
    }

    RawDocumentCandidate map(LegacyRow row) {
        List<String> labels = parseLabels(row.labelsJson());
        String searchable = String.join("\n", row.title(), row.textContent(), String.join(" ", labels));
        return new RawDocumentCandidate(
                "totvs-winthor",
                String.valueOf(row.id()),
                moduleKey(row.sectionPath(), row.sectionName()),
                moduleKey(row.sectionPath(), row.sectionName()),
                value(row.title()),
                value(row.textContent()),
                value(row.htmlContent()),
                row.url(),
                identifiersExtractor.normalizedDelimited(labels),
                identifiersExtractor.routines(searchable),
                identifiersExtractor.errorCodes(searchable),
                metadata(row),
                parseDate(row.createdAt()),
                parseDate(row.updatedAt()),
                parseDate(row.collectedAt()));
    }

    private List<String> parseLabels(String labelsJson) {
        if (labelsJson == null || labelsJson.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(labelsJson, new TypeReference<>() { });
        } catch (JsonProcessingException exception) {
            return List.of();
        }
    }

    private String metadata(LegacyRow row) {
        try {
            return objectMapper.writeValueAsString(objectMapper.createObjectNode()
                    .put("legacySectionId", row.sectionId())
                    .put("legacySectionName", row.sectionName())
                    .put("legacySectionPath", row.sectionPath())
                    .put("legacySourceApi", row.sourceApi()));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Não foi possível registrar metadata legada", exception);
        }
    }

    private OffsetDateTime parseDate(String value) {
        try {
            return value == null || value.isBlank() ? null : OffsetDateTime.parse(value);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private String moduleKey(String sectionPath, String sectionName) {
        String path = value(sectionPath);
        String root = path.isBlank() ? value(sectionName) : path.split("\\s*>\\s*", 2)[0];
        String normalized = root.toLowerCase(Locale.ROOT);
        if (normalized.contains("plano de voo")) return "01-plano-de-voo";
        if (normalized.contains("vendas")) return "03-vendas";
        if (normalized.contains("adm. interna")) return "11-adm-interna-estoque";
        if (normalized.contains("recebimento")) return "13-recebimento-mercadoria";
        if (normalized.contains("faturamento")) return "14-faturamento";
        if (normalized.contains("autosserviço")) return "20-autosservico";
        if (normalized.contains("pdv supermercados")) return "pdv-supermercados";
        if (normalized.contains("reforma tributária")) return "reforma-tributaria-winthor";
        return "outros-winthor";
    }

    private String value(String value) {
        return value != null ? value : "";
    }

    record LegacyRow(
            long id, String url, String title, long sectionId, String sectionName,
            String sectionPath, String labelsJson, String textContent, String htmlContent,
            String createdAt, String updatedAt, String sourceApi, String collectedAt) {
    }
}
