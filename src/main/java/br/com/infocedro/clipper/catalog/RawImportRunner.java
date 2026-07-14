package br.com.infocedro.clipper.catalog;

import java.nio.file.Path;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Executa importações locais sem expor uma rota administrativa sem autenticação. */
@Component
public class RawImportRunner implements ApplicationRunner {

    private final RawImportProperties properties;
    private final RawCollectionLocator locator;
    private final RawKnowledgeImportService importService;

    public RawImportRunner(
            RawImportProperties properties,
            RawCollectionLocator locator,
            RawKnowledgeImportService importService
    ) {
        this.properties = properties;
        this.locator = locator;
        this.importService = importService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!properties.isEnabled()) {
            return;
        }
        if (properties.getSourceType() == null || properties.getScope() == null) {
            throw new IllegalArgumentException("Informe source-type e scope para importar o catálogo bruto");
        }

        Path execution = locator.latest(
                properties.getRawRoot(), properties.getSourceType(), properties.getScope());
        RawImportResult result = importService.importCollection(execution);
        System.out.printf(
                "Importação %s concluída: %d inseridos, %d atualizados, %d inalterados.%n",
                result.collectionId(), result.inserted(), result.updated(), result.unchanged());
    }
}
