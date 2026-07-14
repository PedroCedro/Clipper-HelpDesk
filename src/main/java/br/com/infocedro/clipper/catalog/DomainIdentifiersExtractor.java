package br.com.infocedro.clipper.catalog;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/** Extrai apenas identificadores acompanhados de contexto explícito. */
@Component
public class DomainIdentifiersExtractor {

    private static final Pattern ROUTINE = Pattern.compile("(?i)\\brotina[\\s_-]*(\\d{2,5})\\b");
    private static final Pattern ERROR = Pattern.compile(
            "(?i)\\b(?:erro|rejei[cç][aã]o|c[oó]digo)[\\s:–-]*(\\d{2,6})\\b");

    public String routines(String text) {
        return delimitedMatches(ROUTINE, text);
    }

    public String errorCodes(String text) {
        return delimitedMatches(ERROR, text);
    }

    public String normalizedDelimited(Iterable<String> values) {
        Set<String> normalized = new LinkedHashSet<>();
        values.forEach(value -> normalized.add(normalize(value)));
        return delimit(normalized);
    }

    private String delimitedMatches(Pattern pattern, String text) {
        Set<String> values = new LinkedHashSet<>();
        Matcher matcher = pattern.matcher(text != null ? text : "");
        while (matcher.find()) {
            values.add(matcher.group(1));
        }
        return delimit(values);
    }

    private String delimit(Set<String> values) {
        return values.isEmpty() ? "" : "|" + String.join("|", values) + "|";
    }

    private String normalize(String value) {
        String decomposed = Normalizer.normalize(value != null ? value : "", Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "").toLowerCase().trim();
    }
}
