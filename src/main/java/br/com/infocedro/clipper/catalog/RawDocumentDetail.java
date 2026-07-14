package br.com.infocedro.clipper.catalog;

import java.time.OffsetDateTime;

/** Detalhe seguro para revisão humana; HTML e metadataJson não saem da API. */
public record RawDocumentDetail(
        Long id,
        String externalId,
        String sourceType,
        String scope,
        String module,
        String title,
        String textContent,
        String sourceUrl,
        String labelsText,
        String routinesText,
        String errorCodesText,
        OffsetDateTime sourceCreatedAt,
        OffsetDateTime sourceUpdatedAt,
        OffsetDateTime collectedAt
) {
    public static RawDocumentDetail from(RawKnowledgeDocument document) {
        return new RawDocumentDetail(
                document.getId(), document.getExternalId(), document.getSourceType(),
                document.getScope(), document.getModule(), document.getTitle(),
                document.getTextContent(), document.getSourceUrl(), document.getLabelsText(),
                document.getRoutinesText(), document.getErrorCodesText(),
                document.getSourceCreatedAt(), document.getSourceUpdatedAt(), document.getCollectedAt());
    }
}
