package br.com.infocedro.clipper.knowledge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.InputStream;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class KnowledgeSeeder implements CommandLineRunner {

    // Mapper reutilizado para evitar recriar a configuração YAML a cada execução.
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
    private static final TypeReference<List<KnowledgeArticle>> ARTICLE_LIST_TYPE = new TypeReference<>() {
    };

    private final KnowledgeRepository repository;

    public KnowledgeSeeder(KnowledgeRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) throws Exception {
        // Evita duplicar os artigos sempre que a aplicação iniciar.
        if (repository.count() > 0) {
            return;
        }

        // Carrega o arquivo YAML empacotado em src/main/resources/knowledge.
        ClassPathResource resource = new ClassPathResource("knowledge/artigos.yaml");

        try (InputStream inputStream = resource.getInputStream()) {
            // Converte a lista do YAML em entidades e persiste tudo em lote.
            List<KnowledgeArticle> articles = YAML_MAPPER.readValue(inputStream, ARTICLE_LIST_TYPE);
            repository.saveAll(articles);
        }
    }
}
