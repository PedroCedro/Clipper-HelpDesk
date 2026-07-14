package br.com.infocedro.clipper.curation;

import java.time.OffsetDateTime;

/** Leitura estável do caso sem expor a entidade de persistência. */
public record CurationCaseSnapshot(
        Long id,
        CurationOriginType originType,
        String originReference,
        CurationStatus status,
        String reason,
        String author,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {

    static CurationCaseSnapshot from(CurationCase curationCase) {
        return new CurationCaseSnapshot(
                curationCase.getId(),
                curationCase.getOriginType(),
                curationCase.getOriginReference(),
                curationCase.getStatus(),
                curationCase.getReason(),
                curationCase.getAuthor(),
                curationCase.getCreatedAt(),
                curationCase.getUpdatedAt());
    }
}
