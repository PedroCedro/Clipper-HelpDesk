package br.com.infocedro.clipper.collector;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Registro tipado de seção, mantendo o contrato dos JSONL já coletados. */
public record TotvsSectionRecord(
        long id,
        @JsonProperty("nome") String name,
        String url,
        @JsonProperty("category_id") long categoryId,
        @JsonProperty("parent_section_id") Long parentSectionId,
        @JsonProperty("caminho") String path,
        @JsonProperty("collected_at") String collectedAt
) {
}
