package br.com.infocedro.clipper.catalog;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RawKnowledgeDocumentRepository extends JpaRepository<RawKnowledgeDocument, Long> {

    List<RawKnowledgeDocument> findBySourceTypeAndExternalIdIn(String sourceType, Collection<String> externalIds);
}
