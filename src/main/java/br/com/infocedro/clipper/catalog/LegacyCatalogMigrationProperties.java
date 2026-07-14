package br.com.infocedro.clipper.catalog;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Configuração opt-in da ponte descartável para o banco do coletor antigo. */
@ConfigurationProperties("clipper.catalog.legacy-migration")
public class LegacyCatalogMigrationProperties {

    private boolean enabled;
    private String databasePath = "src/main/java/br/com/infocedro/clipper/collector/totvs-winthor";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDatabasePath() {
        return databasePath;
    }

    public void setDatabasePath(String databasePath) {
        this.databasePath = databasePath;
    }
}
