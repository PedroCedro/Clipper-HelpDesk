package br.com.infocedro.clipper.catalog;

import java.time.OffsetDateTime;

/** Documento neutro, já normalizado pelo adapter e pronto para persistência. */
public record RawDocumentCandidate(
        String sourceType,
        String externalId,
        String scope,
        String module,
        String title,
        String textContent,
        String htmlContent,
        String sourceUrl,
        String labelsText,
        String routinesText,
        String errorCodesText,
        String metadataJson,
        OffsetDateTime sourceCreatedAt,
        OffsetDateTime sourceUpdatedAt,
        OffsetDateTime collectedAt
) {
}
