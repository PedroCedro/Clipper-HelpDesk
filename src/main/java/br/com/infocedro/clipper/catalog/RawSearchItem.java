package br.com.infocedro.clipper.catalog;

import java.time.OffsetDateTime;
import java.util.Set;

/** Resultado ranqueado sem expor o snapshot HTML bruto. */
public record RawSearchItem(
        Long id,
        String externalId,
        String title,
        String module,
        int score,
        Set<RawMatchReason> matchReasons,
        String snippet,
        String sourceUrl,
        OffsetDateTime sourceUpdatedAt
) {
}
