package br.com.infocedro.clipper.catalog;

import java.nio.file.Path;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clipper.catalog.import")
public class RawImportProperties {

    private boolean enabled;
    private String sourceType;
    private String scope;
    private Path rawRoot = Path.of("var/knowledge/raw");

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getScope() { return scope; }
    public void setScope(String scope) { this.scope = scope; }
    public Path getRawRoot() { return rawRoot; }
    public void setRawRoot(Path rawRoot) { this.rawRoot = rawRoot; }
}
