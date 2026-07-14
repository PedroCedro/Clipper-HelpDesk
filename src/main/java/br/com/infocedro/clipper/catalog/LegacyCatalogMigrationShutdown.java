package br.com.infocedro.clipper.catalog;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Encerra o comando somente depois que todos os runners terminaram. */
@Component
class LegacyCatalogMigrationShutdown {

    private final LegacyCatalogMigrationProperties properties;
    private final ConfigurableApplicationContext context;

    LegacyCatalogMigrationShutdown(
            LegacyCatalogMigrationProperties properties,
            ConfigurableApplicationContext context) {
        this.properties = properties;
        this.context = context;
    }

    @EventListener(ApplicationReadyEvent.class)
    void closeMigrationCommand() {
        if (properties.isEnabled()) {
            context.close();
        }
    }
}
