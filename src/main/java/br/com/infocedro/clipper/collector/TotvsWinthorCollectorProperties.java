package br.com.infocedro.clipper.collector;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Opções do crawler TOTVS. Limites iguais a 0 significam "sem limite", útil
// para alternar entre testes pequenos e coleta completa sem mudar código.
@ConfigurationProperties(prefix = "clipper.collector.totvs-winthor")
public class TotvsWinthorCollectorProperties {

    private boolean enabled = false;
    private String module;
    private String locale = "pt-br";
    private Path rawRoot = Path.of("var/knowledge/raw");
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

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }

    public Path getRawRoot() {
        return rawRoot;
    }

    public void setRawRoot(Path rawRoot) {
        this.rawRoot = rawRoot;
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
