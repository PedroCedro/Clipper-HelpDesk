package br.com.infocedro.clipper.collector;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

// Coletor da Central TOTVS/WinThor via API pública do Help Center.
// Saída dupla: JSONL para versionar/inspecionar e H2 opcional para consultas.
@Service
public class TotvsWinthorCrawler {

    private static final String API_HOST = "https://centraldeatendimento.totvs.com";
    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern NUMERIC_ENTITY_PATTERN = Pattern.compile("&#(x?[0-9A-Fa-f]+);");

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final TotvsWinthorCollectorDatabase collectorDatabase;

    public TotvsWinthorCrawler(ObjectMapper objectMapper, TotvsWinthorCollectorDatabase collectorDatabase) {
        this.objectMapper = objectMapper;
        this.collectorDatabase = collectorDatabase;
        this.httpClient = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public CrawlSummary crawl(TotvsWinthorCollectorProperties properties) throws IOException, InterruptedException, SQLException {
        Files.createDirectories(properties.getOutputDir());

        Path sectionsFile = properties.getOutputDir().resolve("sections.jsonl");
        Path articlesFile = properties.getOutputDir().resolve("artigos.jsonl");
        Path summaryFile = properties.getOutputDir().resolve("crawl-summary.json");

        Set<Long> visitedSections = new HashSet<>();
        Set<Long> visitedArticles = new HashSet<>();
        SectionCursor rootSection = fetchRootSection(properties);
        Map<Long, List<SectionCursor>> childSections = loadCategorySectionTree(properties, rootSection);
        Queue<SectionCursor> pendingSections = new ArrayDeque<>();
        pendingSections.add(rootSection);

        int sectionCount = 0;
        int articleCount = 0;

        try (BufferedWriter sectionWriter = Files.newBufferedWriter(sectionsFile, StandardCharsets.UTF_8);
             BufferedWriter articleWriter = Files.newBufferedWriter(articlesFile, StandardCharsets.UTF_8);
             TotvsWinthorCollectorDatabase.DatabaseSession database = collectorDatabase.open(properties)) {
            // Varre a árvore em largura para manter a coleta previsível e respeitar limites.
            while (!pendingSections.isEmpty()) {
                if (isLimitReached(sectionCount, properties.getMaxSections())) {
                    break;
                }

                SectionCursor section = pendingSections.remove();
                if (!visitedSections.add(section.id())) {
                    continue;
                }

                Map<String, Object> sectionRecord = sectionRecord(section);
                writeJsonLine(sectionWriter, sectionRecord);
                database.upsertSection(sectionRecord);
                sectionCount++;

                articleCount += collectArticles(properties, section, articleWriter, database, visitedArticles, articleCount);

                for (SectionCursor subsection : childSections.getOrDefault(section.id(), List.of())) {
                    if (!visitedSections.contains(subsection.id())) {
                        pendingSections.add(subsection);
                    }
                }
            }
        }

        CrawlSummary summary = new CrawlSummary(
                sectionCount,
                articleCount,
                sectionsFile.toString(),
                articlesFile.toString(),
                properties.isDatabaseEnabled() ? properties.getDatabasePath().toString() + ".mv.db" : null,
                OffsetDateTime.now().toString()
        );
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(summaryFile.toFile(), summary);
        return summary;
    }

    private int collectArticles(
            TotvsWinthorCollectorProperties properties,
            SectionCursor section,
            BufferedWriter articleWriter,
            TotvsWinthorCollectorDatabase.DatabaseSession database,
            Set<Long> visitedArticles,
            int currentTotal
    ) throws IOException, InterruptedException, SQLException {
        int collected = 0;
        String nextPage = articlesUrl(properties, section.id());

        while (nextPage != null && !isLimitReached(currentTotal + collected, properties.getMaxArticles())) {
            JsonNode payload = fetchJson(nextPage, properties.getRequestDelayMillis());
            for (JsonNode article : payload.path("articles")) {
                if (isLimitReached(currentTotal + collected, properties.getMaxArticles())) {
                    break;
                }

                long articleId = article.path("id").asLong();
                if (!visitedArticles.add(articleId)) {
                    continue;
                }

                Map<String, Object> articleRecord = articleRecord(article, section, nextPage);
                writeJsonLine(articleWriter, articleRecord);
                database.upsertArticle(articleRecord);
                collected++;
            }
            nextPage = textOrNull(payload.path("next_page"));
        }

        return collected;
    }

    private JsonNode fetchJson(String url, long delayMillis) throws IOException, InterruptedException {
        if (delayMillis > 0) {
            Thread.sleep(delayMillis);
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .header("User-Agent", "ClipperTotvsWinthorCrawler/0.1")
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("TOTVS API returned HTTP " + response.statusCode() + " for " + url);
        }

        return objectMapper.readTree(response.body());
    }

    private Map<String, Object> sectionRecord(SectionCursor section) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id", section.id());
        record.put("nome", section.name());
        record.put("url", section.htmlUrl());
        record.put("category_id", section.categoryId());
        record.put("parent_section_id", section.parentId());
        record.put("caminho", section.path());
        record.put("collected_at", OffsetDateTime.now().toString());
        return record;
    }

    private Map<String, Object> articleRecord(JsonNode article, SectionCursor section, String sourceApi) {
        String body = textOrDefault(article.path("body"), "");

        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id", article.path("id").asLong());
        record.put("url", textOrNull(article.path("html_url")));
        record.put("titulo", textOrDefault(article.path("title"), ""));
        record.put("secao_id", section.id());
        record.put("secao_nome", section.name());
        record.put("secao_caminho", section.path());
        record.put("labels", objectMapper.convertValue(article.path("label_names"), List.class));
        record.put("conteudo_texto", htmlToText(body));
        record.put("conteudo_html", body);
        record.put("criado_em", textOrNull(article.path("created_at")));
        record.put("atualizado_em", textOrNull(article.path("updated_at")));
        record.put("source_api", sourceApi);
        record.put("collected_at", OffsetDateTime.now().toString());
        return record;
    }

    private String articlesUrl(TotvsWinthorCollectorProperties properties, long sectionId) {
        return API_HOST + "/api/v2/help_center/" + encode(properties.getLocale())
                + "/sections/" + sectionId + "/articles.json?per_page=" + properties.getPerPage();
    }

    private SectionCursor fetchRootSection(TotvsWinthorCollectorProperties properties) throws IOException, InterruptedException {
        JsonNode payload = fetchJson(sectionUrl(properties, properties.getRootSectionId()), properties.getRequestDelayMillis());
        JsonNode section = payload.path("section");
        String name = textOrDefault(section.path("name"), properties.getRootSectionName());
        return new SectionCursor(
                section.path("id").asLong(properties.getRootSectionId()),
                name,
                section.path("category_id").asLong(),
                null,
                textOrNull(section.path("html_url")),
                name
        );
    }

    private Map<Long, List<SectionCursor>> loadCategorySectionTree(
            TotvsWinthorCollectorProperties properties,
            SectionCursor rootSection
    ) throws IOException, InterruptedException {
        List<JsonNode> categorySections = loadCategorySections(properties, rootSection.categoryId());
        Map<Long, List<JsonNode>> rawChildren = new LinkedHashMap<>();

        // A API lista seções por categoria; reconstruímos a árvore usando parent_section_id.
        for (JsonNode section : categorySections) {
            Long parentId = longOrNull(section.path("parent_section_id"));
            if (parentId == null) {
                continue;
            }
            rawChildren.computeIfAbsent(parentId, ignored -> new ArrayList<>()).add(section);
        }

        Map<Long, List<SectionCursor>> descendantsByParent = new LinkedHashMap<>();
        Queue<SectionCursor> parents = new ArrayDeque<>();
        parents.add(rootSection);

        while (!parents.isEmpty()) {
            SectionCursor parent = parents.remove();
            List<SectionCursor> children = new ArrayList<>();
            for (JsonNode child : rawChildren.getOrDefault(parent.id(), List.of())) {
                long sectionId = child.path("id").asLong();
                String name = textOrDefault(child.path("name"), "Section " + sectionId);
                SectionCursor cursor = new SectionCursor(
                        sectionId,
                        name,
                        child.path("category_id").asLong(rootSection.categoryId()),
                        parent.id(),
                        textOrNull(child.path("html_url")),
                        parent.path() + " > " + name
                );
                children.add(cursor);
                parents.add(cursor);
            }
            descendantsByParent.put(parent.id(), children);
        }

        return descendantsByParent;
    }

    private List<JsonNode> loadCategorySections(
            TotvsWinthorCollectorProperties properties,
            long categoryId
    ) throws IOException, InterruptedException {
        List<JsonNode> sections = new ArrayList<>();
        String nextPage = categorySectionsUrl(properties, categoryId);

        while (nextPage != null) {
            JsonNode payload = fetchJson(nextPage, properties.getRequestDelayMillis());
            for (JsonNode section : payload.path("sections")) {
                sections.add(section);
            }
            nextPage = textOrNull(payload.path("next_page"));
        }

        return sections;
    }

    private String sectionUrl(TotvsWinthorCollectorProperties properties, long sectionId) {
        return API_HOST + "/api/v2/help_center/" + encode(properties.getLocale())
                + "/sections/" + sectionId + ".json";
    }

    private String categorySectionsUrl(TotvsWinthorCollectorProperties properties, long categoryId) {
        return API_HOST + "/api/v2/help_center/" + encode(properties.getLocale())
                + "/categories/" + categoryId + "/sections.json?per_page=" + properties.getPerPage();
    }

    private void writeJsonLine(BufferedWriter writer, Map<String, Object> record) throws IOException {
        writer.write(objectMapper.writeValueAsString(record));
        writer.newLine();
        writer.flush();
    }

    private boolean isLimitReached(int currentTotal, int max) {
        return max > 0 && currentTotal >= max;
    }

    private String textOrDefault(JsonNode node, String defaultValue) {
        String value = textOrNull(node);
        return value == null ? defaultValue : value;
    }

    private String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.asText();
    }

    private Long longOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.asLong();
    }

    private String htmlToText(String html) {
        if (html == null || html.isBlank()) {
            return "";
        }

        // Conversão leve: suficiente para indexar texto limpo sem depender de parser HTML.
        String text = html
                .replaceAll("(?i)<br\\s*/?>", "\n")
                .replaceAll("(?i)</p>", "\n")
                .replaceAll("(?i)</li>", "\n")
                .replaceAll("(?i)</h[1-6]>", "\n")
                .replaceAll("(?i)</tr>", "\n");
        text = TAG_PATTERN.matcher(text).replaceAll(" ");
        text = decodeHtmlEntities(text);
        return text.replace('\u00A0', ' ')
                .replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String decodeHtmlEntities(String text) {
        String withNamedEntities = text
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&nbsp;", " ");

        Matcher matcher = NUMERIC_ENTITY_PATTERN.matcher(withNamedEntities);
        StringBuilder decoded = new StringBuilder();
        while (matcher.find()) {
            String token = matcher.group(1);
            int codePoint = token.startsWith("x") || token.startsWith("X")
                    ? Integer.parseInt(token.substring(1), 16)
                    : Integer.parseInt(token);
            matcher.appendReplacement(decoded, Matcher.quoteReplacement(new String(Character.toChars(codePoint))));
        }
        matcher.appendTail(decoded);
        return decoded.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(Objects.requireNonNull(value), StandardCharsets.UTF_8);
    }

    private record SectionCursor(long id, String name, long categoryId, Long parentId, String htmlUrl, String path) {
    }

    public record CrawlSummary(
            int sections,
            int articles,
            String sectionsFile,
            String articlesFile,
            String databaseFile,
            String collectedAt
    ) {
    }
}
