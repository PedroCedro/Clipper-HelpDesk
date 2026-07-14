package br.com.infocedro.clipper.catalog;

import java.util.Comparator;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

/** Pesquisa o catálogo bruto e explica por que cada documento pontuou. */
@Service
public class RawKnowledgeSearchService {

    private static final int MAX_PAGE_SIZE = 50;
    private static final int SNIPPET_LENGTH = 180;
    private static final Set<String> STOPWORDS = Set.of("com", "para", "não", "nao", "uma", "que");

    private final RawKnowledgeDocumentRepository repository;

    public RawKnowledgeSearchService(RawKnowledgeDocumentRepository repository) {
        this.repository = repository;
    }

    public RawSearchPage search(RawSearchQuery query) {
        validate(query);
        Set<String> tokens = tokenize(query.text());
        if (tokens.isEmpty()) {
            return new RawSearchPage(List.of(), query.page(), query.size(), 0);
        }

        Map<String, Pattern> numericPatterns = numericPatterns(tokens);
        Map<Long, RawSearchCandidate> uniqueCandidates = new LinkedHashMap<>();
        for (String token : tokens) {
            repository.searchCandidates(token, blankToNull(query.sourceType()), blankToNull(query.module()))
                    .forEach(candidate -> uniqueCandidates.putIfAbsent(candidate.getId(), candidate));
        }

        List<RawSearchItem> ranked = uniqueCandidates.values().stream()
                .map(candidate -> rank(candidate, tokens, numericPatterns))
                .filter(item -> item.score() > 0)
                .sorted(Comparator.comparingInt(RawSearchItem::score).reversed()
                        .thenComparing(RawSearchItem::sourceUpdatedAt,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(RawSearchItem::title, String.CASE_INSENSITIVE_ORDER))
                .toList();

        long offset = (long) query.page() * query.size();
        int from = (int) Math.min(offset, ranked.size());
        int to = Math.min(from + query.size(), ranked.size());
        return new RawSearchPage(List.copyOf(ranked.subList(from, to)), query.page(), query.size(), ranked.size());
    }

    private RawSearchItem rank(
            RawSearchCandidate candidate,
            Set<String> tokens,
            Map<String, Pattern> numericPatterns
    ) {
        String title = lower(candidate.getTitle());
        String labels = lower(candidate.getLabelsText());
        String content = lower(candidate.getTextContent());
        String routines = value(candidate.getRoutinesText());
        String errors = value(candidate.getErrorCodesText());
        EnumSet<RawMatchReason> reasons = EnumSet.noneOf(RawMatchReason.class);
        int score = 0;

        for (String token : tokens) {
            String delimited = "|" + token + "|";
            if (routines.contains(delimited) || errors.contains(delimited)) {
                score += 10;
                reasons.add(RawMatchReason.EXACT_IDENTIFIER);
            }
            if (containsToken(title, token, numericPatterns)) {
                score += 3;
                reasons.add(RawMatchReason.TITLE);
            }
            if (containsToken(labels, token, numericPatterns)) {
                score += 2;
                reasons.add(RawMatchReason.LABEL);
            }
            if (containsToken(content, token, numericPatterns)) {
                score += 1;
                reasons.add(RawMatchReason.CONTENT);
            }
        }

        return new RawSearchItem(
                candidate.getId(), candidate.getExternalId(), candidate.getTitle(), candidate.getModule(),
                score, Collections.unmodifiableSet(EnumSet.copyOf(reasons)),
                snippet(candidate.getTextContent(), tokens),
                candidate.getSourceUrl(), candidate.getSourceUpdatedAt());
    }

    private String snippet(String content, Set<String> tokens) {
        String safeContent = value(content).replaceAll("\\s+", " ").trim();
        if (safeContent.length() <= SNIPPET_LENGTH) {
            return safeContent;
        }
        String normalized = lower(safeContent);
        int firstMatch = tokens.stream().mapToInt(normalized::indexOf).filter(index -> index >= 0).min().orElse(0);
        int start = Math.max(0, firstMatch - 50);
        int end = Math.min(safeContent.length(), start + SNIPPET_LENGTH);
        return (start > 0 ? "…" : "") + safeContent.substring(start, end) + (end < safeContent.length() ? "…" : "");
    }

    private Set<String> tokenize(String text) {
        Set<String> tokens = new LinkedHashSet<>();
        for (String token : lower(text).split("[^\\p{L}\\p{N}]+")) {
            if (token.length() >= 2 && !STOPWORDS.contains(token)) {
                tokens.add(token);
            }
        }
        return tokens;
    }

    private void validate(RawSearchQuery query) {
        if (query == null || query.text() == null || query.text().trim().length() < 2) {
            throw new IllegalArgumentException("A busca deve ter ao menos 2 caracteres");
        }
        if (query.page() < 0) {
            throw new IllegalArgumentException("A página não pode ser negativa");
        }
        if (query.size() < 1 || query.size() > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("O tamanho da página deve ficar entre 1 e 50");
        }
    }

    private Map<String, Pattern> numericPatterns(Set<String> tokens) {
        Map<String, Pattern> patterns = new LinkedHashMap<>();
        tokens.stream()
                .filter(token -> token.chars().allMatch(Character::isDigit))
                .forEach(token -> patterns.put(
                        token, Pattern.compile("(?<!\\d)" + Pattern.quote(token) + "(?!\\d)")));
        return Map.copyOf(patterns);
    }

    private boolean containsToken(String text, String token, Map<String, Pattern> numericPatterns) {
        if (!token.chars().allMatch(Character::isDigit)) {
            return text.contains(token);
        }
        // Números de rotina e rejeição não podem casar por prefixo: 144 é
        // diferente de 1443 mesmo quando aparece no título ou no corpo.
        return numericPatterns.get(token).matcher(text).find();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String lower(String value) {
        return value(value).toLowerCase(Locale.ROOT);
    }

    private String value(String value) {
        return value != null ? value : "";
    }
}
