package br.com.infocedro.clipper.catalog;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Snapshot externo pesquisável; não representa conhecimento aprovado. */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(
        name = "uk_raw_document_source_external", columnNames = {"source_type", "external_id"}))
public class RawKnowledgeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "source_type", nullable = false, length = 100)
    private String sourceType;
    @Column(name = "external_id", nullable = false, length = 255)
    private String externalId;
    @Column(nullable = false, length = 255)
    private String scope;
    @Column(length = 255)
    private String module;
    @Column(nullable = false, length = 2000)
    private String title;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String textContent;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String htmlContent;
    @Column(length = 2048)
    private String sourceUrl;
    @Column(columnDefinition = "TEXT")
    private String labelsText;
    @Column(columnDefinition = "TEXT")
    private String routinesText;
    @Column(columnDefinition = "TEXT")
    private String errorCodesText;
    @Column(columnDefinition = "TEXT", nullable = false)
    private String metadataJson;
    private OffsetDateTime sourceCreatedAt;
    private OffsetDateTime sourceUpdatedAt;
    private OffsetDateTime collectedAt;
    @Column(nullable = false, length = 64)
    private String contentHash;
    @Column(nullable = false)
    private OffsetDateTime importedAt;
    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    protected RawKnowledgeDocument() {
    }

    public static RawKnowledgeDocument create(
            RawDocumentCandidate candidate, String contentHash, OffsetDateTime now) {
        RawKnowledgeDocument document = new RawKnowledgeDocument();
        document.sourceType = candidate.sourceType();
        document.externalId = candidate.externalId();
        document.importedAt = now;
        document.apply(candidate, contentHash, now);
        return document;
    }

    public void apply(RawDocumentCandidate candidate, String contentHash, OffsetDateTime now) {
        scope = candidate.scope();
        module = candidate.module();
        title = candidate.title();
        textContent = candidate.textContent();
        htmlContent = candidate.htmlContent();
        sourceUrl = candidate.sourceUrl();
        labelsText = candidate.labelsText();
        routinesText = candidate.routinesText();
        errorCodesText = candidate.errorCodesText();
        metadataJson = candidate.metadataJson();
        sourceCreatedAt = candidate.sourceCreatedAt();
        sourceUpdatedAt = candidate.sourceUpdatedAt();
        collectedAt = candidate.collectedAt();
        this.contentHash = contentHash;
        updatedAt = now;
    }

    public Long getId() { return id; }
    public String getSourceType() { return sourceType; }
    public String getExternalId() { return externalId; }
    public String getTitle() { return title; }
    public String getTextContent() { return textContent; }
    public String getHtmlContent() { return htmlContent; }
    public String getLabelsText() { return labelsText; }
    public String getRoutinesText() { return routinesText; }
    public String getErrorCodesText() { return errorCodesText; }
    public String getContentHash() { return contentHash; }

    public String getScope() { return scope; }
    public String getModule() { return module; }
    public String getSourceUrl() { return sourceUrl; }
    public OffsetDateTime getSourceCreatedAt() { return sourceCreatedAt; }
    public OffsetDateTime getSourceUpdatedAt() { return sourceUpdatedAt; }
    public OffsetDateTime getCollectedAt() { return collectedAt; }
}
