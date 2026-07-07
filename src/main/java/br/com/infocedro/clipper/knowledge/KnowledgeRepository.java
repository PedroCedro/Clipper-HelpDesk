package br.com.infocedro.clipper.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

// Busca inicial por palavras-chave. No futuro pode virar busca full-text,
// embeddings ou ranking híbrido sem mudar quem consome o repositório.
public interface KnowledgeRepository extends JpaRepository<KnowledgeArticle, Long> {

    List<KnowledgeArticle> findByKeywordsContainingIgnoreCase(String term);

}
