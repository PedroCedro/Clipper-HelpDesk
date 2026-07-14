package br.com.infocedro.clipper.collector;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.URLEncoder;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

/**
 * Orquestra a coleta WinThor: percorre seções, evita repetições e encaminha
 * registros tipados para as saídas. HTTP, tradução e limpeza ficam em
 * colaboradores próprios para que este fluxo permaneça legível.
 */
@Service
public class TotvsWinthorCrawler {

    private static final String API_HOST = "https://centraldeatendimento.totvs.com";

    private final ObjectMapper objectMapper;
    private final TotvsHelpCenterClient client;
    private final TotvsWinthorDocumentMapper documentMapper;
    private final TotvsWinthorCollectorDatabase collectorDatabase;

    public TotvsWinthorCrawler(
            ObjectMapper objectMapper,
            TotvsHelpCenterClient client,
            TotvsWinthorDocumentMapper documentMapper,
            TotvsWinthorCollectorDatabase collectorDatabase
    ) {
        this.objectMapper = objectMapper;
        this.client = client;
        this.documentMapper = documentMapper;
        this.collectorDatabase = collectorDatabase;
    }

    public CrawlSummary crawl(TotvsWinthorCollectorProperties properties)
            throws IOException, InterruptedException, SQLException {
        Files.createDirectories(properties.getOutputDir());
        Path sectionsFile = properties.getOutputDir().resolve("sections.jsonl");
        Path articlesFile = properties.getOutputDir().resolve("artigos.jsonl");
        Path summaryFile = properties.getOutputDir().resolve("crawl-summary.json");

        Set<Long> visitedSections = new HashSet<>();
        Set<Long> visitedArticles = new HashSet<>();
        SectionCursor root = fetchRootSection(properties);
        Map<Long, List<SectionCursor>> children = loadSectionTree(properties, root);
        Queue<SectionCursor> pending = new ArrayDeque<>();
        pending.add(root);
        int sectionCount = 0;
        int articleCount = 0;

        try (BufferedWriter sectionWriter = Files.newBufferedWriter(sectionsFile, StandardCharsets.UTF_8);
             BufferedWriter articleWriter = Files.newBufferedWriter(articlesFile, StandardCharsets.UTF_8);
             TotvsWinthorCollectorDatabase.DatabaseSession database = collectorDatabase.open(properties)) {
            // Busca em largura mantém a ordem previsível e permite interromper
            // a coleta por limite sem perder a noção da hierarquia visitada.
            while (!pending.isEmpty() && !limitReached(sectionCount, properties.getMaxSections())) {
                SectionCursor section = pending.remove();
                if (!visitedSections.add(section.id())) {
                    continue;
                }

                TotvsSectionRecord sectionRecord = documentMapper.section(section);
                writeJsonLine(sectionWriter, sectionRecord);
                database.upsertSection(sectionRecord);
                sectionCount++;

                articleCount += collectArticles(
                        properties, section, articleWriter, database, visitedArticles, articleCount);
                children.getOrDefault(section.id(), List.of()).stream()
                        .filter(child -> !visitedSections.contains(child.id()))
                        .forEach(pending::add);
            }
        }

        CrawlSummary summary = new CrawlSummary(
                sectionCount, articleCount, sectionsFile.toString(), articlesFile.toString(),
                properties.isDatabaseEnabled() ? properties.getDatabasePath() + ".mv.db" : null,
                OffsetDateTime.now().toString());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(summaryFile.toFile(), summary);
        return summary;
    }

    private int collectArticles(
            TotvsWinthorCollectorProperties properties,
            SectionCursor section,
            BufferedWriter writer,
            TotvsWinthorCollectorDatabase.DatabaseSession database,
            Set<Long> visitedArticles,
            int currentTotal
    ) throws IOException, InterruptedException, SQLException {
        int collected = 0;
        String nextPage = articlesUrl(properties, section.id());
        while (nextPage != null && !limitReached(currentTotal + collected, properties.getMaxArticles())) {
            String sourceApi = nextPage;
            JsonNode payload = client.get(sourceApi, properties.getRequestDelayMillis());
            for (JsonNode article : payload.path("articles")) {
                if (limitReached(currentTotal + collected, properties.getMaxArticles())) {
                    break;
                }
                if (!visitedArticles.add(article.path("id").asLong())) {
                    continue;
                }

                TotvsArticleRecord record = documentMapper.article(article, section, sourceApi);
                writeJsonLine(writer, record);
                database.upsertArticle(record);
                collected++;
            }
            nextPage = textOrNull(payload.path("next_page"));
        }
        return collected;
    }

    private SectionCursor fetchRootSection(TotvsWinthorCollectorProperties properties)
            throws IOException, InterruptedException {
        JsonNode section = client.get(
                sectionUrl(properties, properties.getRootSectionId()),
                properties.getRequestDelayMillis()).path("section");
        String name = textOrNull(section.path("name"));
        return new SectionCursor(
                section.path("id").asLong(properties.getRootSectionId()),
                name != null ? name : properties.getRootSectionName(),
                section.path("category_id").asLong(), null,
                textOrNull(section.path("html_url")),
                name != null ? name : properties.getRootSectionName());
    }

    private Map<Long, List<SectionCursor>> loadSectionTree(
            TotvsWinthorCollectorProperties properties,
            SectionCursor root
    ) throws IOException, InterruptedException {
        Map<Long, List<JsonNode>> rawChildren = new LinkedHashMap<>();
        for (JsonNode section : loadCategorySections(properties, root.categoryId())) {
            Long parentId = longOrNull(section.path("parent_section_id"));
            if (parentId != null) {
                rawChildren.computeIfAbsent(parentId, ignored -> new ArrayList<>()).add(section);
            }
        }

        Map<Long, List<SectionCursor>> tree = new LinkedHashMap<>();
        Queue<SectionCursor> parents = new ArrayDeque<>();
        parents.add(root);
        while (!parents.isEmpty()) {
            SectionCursor parent = parents.remove();
            List<SectionCursor> children = rawChildren.getOrDefault(parent.id(), List.of()).stream()
                    .map(child -> childCursor(child, parent, root.categoryId()))
                    .toList();
            tree.put(parent.id(), children);
            parents.addAll(children);
        }
        return tree;
    }

    private List<JsonNode> loadCategorySections(TotvsWinthorCollectorProperties properties, long categoryId)
            throws IOException, InterruptedException {
        List<JsonNode> sections = new ArrayList<>();
        String nextPage = categorySectionsUrl(properties, categoryId);
        while (nextPage != null) {
            JsonNode payload = client.get(nextPage, properties.getRequestDelayMillis());
            payload.path("sections").forEach(sections::add);
            nextPage = textOrNull(payload.path("next_page"));
        }
        return sections;
    }

    private SectionCursor childCursor(JsonNode child, SectionCursor parent, long defaultCategoryId) {
        long id = child.path("id").asLong();
        String name = textOrNull(child.path("name"));
        name = name != null ? name : "Section " + id;
        return new SectionCursor(
                id, name, child.path("category_id").asLong(defaultCategoryId), parent.id(),
                textOrNull(child.path("html_url")), parent.path() + " > " + name);
    }

    private String sectionUrl(TotvsWinthorCollectorProperties properties, long sectionId) {
        return API_HOST + "/api/v2/help_center/" + encode(properties.getLocale())
                + "/sections/" + sectionId + ".json";
    }

    private String categorySectionsUrl(TotvsWinthorCollectorProperties properties, long categoryId) {
        return API_HOST + "/api/v2/help_center/" + encode(properties.getLocale())
                + "/categories/" + categoryId + "/sections.json?per_page=" + properties.getPerPage();
    }

    private String articlesUrl(TotvsWinthorCollectorProperties properties, long sectionId) {
        return API_HOST + "/api/v2/help_center/" + encode(properties.getLocale())
                + "/sections/" + sectionId + "/articles.json?per_page=" + properties.getPerPage();
    }

    private void writeJsonLine(BufferedWriter writer, Object record) throws IOException {
        writer.write(objectMapper.writeValueAsString(record));
        writer.newLine();
        writer.flush();
    }

    private boolean limitReached(int current, int max) {
        return max > 0 && current >= max;
    }

    private String textOrNull(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asText();
    }

    private Long longOrNull(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asLong();
    }

    private String encode(String value) {
        return URLEncoder.encode(Objects.requireNonNull(value), StandardCharsets.UTF_8);
    }

    record SectionCursor(long id, String name, long categoryId, Long parentId, String htmlUrl, String path) {
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
