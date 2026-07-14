package br.com.infocedro.clipper.catalog;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RawKnowledgeDocumentRepository extends JpaRepository<RawKnowledgeDocument, Long> {

    List<RawKnowledgeDocument> findBySourceTypeAndExternalIdIn(String sourceType, Collection<String> externalIds);

    @Query("""
            select d.id as id, d.externalId as externalId, d.contentHash as contentHash
              from RawKnowledgeDocument d
             where d.sourceType = :sourceType
            """)
    List<RawDocumentIdentityProjection> findIdentitiesBySourceType(@Param("sourceType") String sourceType);

    @Query("""
            select d.id as id, d.sourceType as sourceType, d.title as title,
                   d.module as module, d.sourceUrl as sourceUrl
              from RawKnowledgeDocument d
             where d.id in :ids
            """)
    List<RawDocumentSummaryProjection> findSummariesByIdIn(@Param("ids") Collection<Long> ids);

    // Uma consulta por token reduz o conjunto antes do ranking em Java. A
    // projeção evita transportar snapshots HTML que não participam da busca.
    @Query("""
            select d.id as id, d.externalId as externalId, d.title as title,
                   d.textContent as textContent, d.sourceUrl as sourceUrl,
                   d.labelsText as labelsText, d.routinesText as routinesText,
                   d.errorCodesText as errorCodesText, d.module as module,
                   d.sourceUpdatedAt as sourceUpdatedAt
              from RawKnowledgeDocument d
             where (:sourceType is null or d.sourceType = :sourceType)
               and (:module is null or d.module = :module)
               and (lower(d.title) like lower(concat('%', :term, '%'))
                 or lower(coalesce(d.labelsText, '')) like lower(concat('%', :term, '%'))
                 or lower(d.textContent) like lower(concat('%', :term, '%'))
                 or coalesce(d.routinesText, '') like concat('%|', :term, '|%')
                 or coalesce(d.errorCodesText, '') like concat('%|', :term, '|%'))
            """)
    List<RawSearchCandidate> searchCandidates(
            @Param("term") String term,
            @Param("sourceType") String sourceType,
            @Param("module") String module);
}
