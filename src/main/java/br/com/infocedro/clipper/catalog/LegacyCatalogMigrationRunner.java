package br.com.infocedro.clipper.catalog;

import java.nio.file.Path;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Disponibiliza a migração como comando opt-in, nunca como endpoint HTTP. */
@Component
public class LegacyCatalogMigrationRunner implements ApplicationRunner {

    private final LegacyCatalogMigrationProperties properties;
    private final LegacyCatalogMigrationService migrationService;

    public LegacyCatalogMigrationRunner(
            LegacyCatalogMigrationProperties properties,
            LegacyCatalogMigrationService migrationService) {
        this.properties = properties;
        this.migrationService = migrationService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!properties.isEnabled()) {
            return;
        }
        LegacyCatalogMigrationResult result = migrationService.migrate(Path.of(properties.getDatabasePath()));
        System.out.printf(
                "Migração legada concluída: %d inseridos, %d atualizados, %d inalterados.%n",
                result.inserted(), result.updated(), result.unchanged());
    }
}
