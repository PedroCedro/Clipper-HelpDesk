package br.com.infocedro.clipper.curation;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Vínculo ativo; remoções são preservadas separadamente na trilha de eventos. */
@Entity
@Table(name = "curation_candidate", uniqueConstraints = @UniqueConstraint(
        name = "uk_curation_candidate_case_document",
        columnNames = {"curation_case_id", "document_id"}))
public class CurationCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curation_case_id", nullable = false)
    private CurationCase curationCase;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Column(nullable = false, length = 255)
    private String author;

    @Column(nullable = false, length = 2000)
    private String reason;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    protected CurationCandidate() {
    }

    static CurationCandidate add(
            CurationCase curationCase, Long documentId, String actor, String reason, OffsetDateTime now) {
        CurationCandidate candidate = new CurationCandidate();
        candidate.curationCase = curationCase;
        candidate.documentId = documentId;
        candidate.author = actor.trim();
        candidate.reason = reason.trim();
        candidate.createdAt = now;
        return candidate;
    }

    public Long getId() {
        return id;
    }

    public Long getCurationCaseId() {
        return curationCase.getId();
    }

    public Long getDocumentId() {
        return documentId;
    }

    public String getAuthor() {
        return author;
    }

    public String getReason() {
        return reason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
