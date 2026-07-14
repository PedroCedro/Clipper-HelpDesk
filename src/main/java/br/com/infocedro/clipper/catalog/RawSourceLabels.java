package br.com.infocedro.clipper.catalog;

import java.util.Map;

/** Traduz tipos internos para rótulos neutros definidos pela borda do produto. */
final class RawSourceLabels {

    private static final String DEFAULT_LABEL = "Fonte oficial";
    private static final Map<String, String> LABELS = Map.of(
            "totvs-winthor", DEFAULT_LABEL);

    private RawSourceLabels() {
    }

    static String labelFor(String sourceType) {
        return LABELS.getOrDefault(sourceType, DEFAULT_LABEL);
    }
}
