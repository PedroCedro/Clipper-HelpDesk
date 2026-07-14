package br.com.infocedro.clipper.collector;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Lê a versão oficial empacotada a partir da fonte única `VERSION`. */
@Component
public class ApplicationVersion {

    private static final String RESOURCE = "VERSION";

    public String value() {
        try {
            ClassPathResource resource = new ClassPathResource(RESOURCE);
            return resource.getContentAsString(StandardCharsets.UTF_8).trim();
        } catch (IOException exception) {
            throw new IllegalStateException("Não foi possível ler a versão da aplicação", exception);
        }
    }
}
