package br.com.infocedro.clipper.knowledge;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface KnowledgeRepository extends JpaRepository<KnowledgeArticle, Long> {

    List<KnowledgeArticle> findByKeywordsContainingIgnoreCase(String term);

}
