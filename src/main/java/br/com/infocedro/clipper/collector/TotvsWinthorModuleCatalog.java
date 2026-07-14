package br.com.infocedro.clipper.collector;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Catálogo versionado dos módulos WinThor. Os IDs da fonte ficam no adapter,
 * sem vazar para o script de execução nem para o crawler genérico.
 */
@Component
public class TotvsWinthorModuleCatalog {

    private static final String RESOURCE = "collector/totvs-winthor-modules.yaml";
    private static final TypeReference<List<TotvsWinthorModule>> MODULE_LIST = new TypeReference<>() {
    };

    private final Map<String, TotvsWinthorModule> modules;

    public TotvsWinthorModuleCatalog() throws IOException {
        ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
        try (InputStream input = new ClassPathResource(RESOURCE).getInputStream()) {
            List<TotvsWinthorModule> loaded = yamlMapper.readValue(input, MODULE_LIST);
            Map<String, TotvsWinthorModule> indexed = new LinkedHashMap<>();
            for (TotvsWinthorModule module : loaded) {
                if (indexed.putIfAbsent(module.key(), module) != null) {
                    throw new IllegalStateException("Módulo TOTVS duplicado no catálogo: " + module.key());
                }
            }
            modules = Map.copyOf(indexed);
        }
    }

    public TotvsWinthorModule require(String key) {
        TotvsWinthorModule module = modules.get(key);
        if (module == null) {
            throw new IllegalArgumentException(
                    "Módulo TOTVS não registrado: " + key + ". Disponíveis: " + String.join(", ", modules.keySet()));
        }
        return module;
    }

    public List<TotvsWinthorModule> all() {
        return List.copyOf(modules.values());
    }
}
