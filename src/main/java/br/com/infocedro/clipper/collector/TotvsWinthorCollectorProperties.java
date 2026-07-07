package br.com.infocedro.clipper.collector;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Opções do crawler TOTVS. Limites iguais a 0 significam "sem limite", útil
// para alternar entre testes pequenos e coleta completa sem mudar código.
@ConfigurationProperties(prefix = "clipper.collector.totvs-winthor")
public class TotvsWinthorCollectorProperties {

    private boolean enabled = false;
    private String locale = "pt-br";
    private long rootSectionId = 1500000596481L;
    private String rootSectionName = "Linha Winthor";
    private Path outputDir = Path.of("src/main/java/br/com/infocedro/clipper/collector");
    private boolean databaseEnabled = true;
    private Path databasePath = Path.of("src/main/java/br/com/infocedro/clipper/collector/totvs-winthor");
    private int perPage = 100;
    private long requestDelayMillis = 250;
    private int maxSections = 0;
    private int maxArticles = 0;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public long getRootSectionId() {
        return rootSectionId;
    }

    public void setRootSectionId(long rootSectionId) {
        this.rootSectionId = rootSectionId;
    }

    public String getRootSectionName() {
        return rootSectionName;
    }

    public void setRootSectionName(String rootSectionName) {
        this.rootSectionName = rootSectionName;
    }

    public Path getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(Path outputDir) {
        this.outputDir = outputDir;
    }

    public boolean isDatabaseEnabled() {
        return databaseEnabled;
    }

    public void setDatabaseEnabled(boolean databaseEnabled) {
        this.databaseEnabled = databaseEnabled;
    }

    public Path getDatabasePath() {
        return databasePath;
    }

    public void setDatabasePath(Path databasePath) {
        this.databasePath = databasePath;
    }

    public int getPerPage() {
        return perPage;
    }

    public void setPerPage(int perPage) {
        this.perPage = perPage;
    }

    public long getRequestDelayMillis() {
        return requestDelayMillis;
    }

    public void setRequestDelayMillis(long requestDelayMillis) {
        this.requestDelayMillis = requestDelayMillis;
    }

    public int getMaxSections() {
        return maxSections;
    }

    public void setMaxSections(int maxSections) {
        this.maxSections = maxSections;
    }

    public int getMaxArticles() {
        return maxArticles;
    }

    public void setMaxArticles(int maxArticles) {
        this.maxArticles = maxArticles;
    }
}
