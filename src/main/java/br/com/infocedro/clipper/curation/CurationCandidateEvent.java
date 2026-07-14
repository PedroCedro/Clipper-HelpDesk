package br.com.infocedro.clipper.curation;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Evento imutável que permite reconstruir todos os ciclos de associação. */
@Entity
@Table(name = "curation_candidate_event")
public class CurationCandidateEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curation_case_id", nullable = false)
    private CurationCase curationCase;

    @Column(name = "document_id", nullable = false)
    private Long documentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CurationCandidateEventType eventType;

    @Column(nullable = false, length = 255)
    private String actor;

    @Column(nullable = false, length = 2000)
    private String reason;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    protected CurationCandidateEvent() {
    }

    static CurationCandidateEvent record(
            CurationCase curationCase,
            Long documentId,
            CurationCandidateEventType eventType,
            String actor,
            String reason,
            OffsetDateTime now
    ) {
        CurationCandidateEvent event = new CurationCandidateEvent();
        event.curationCase = curationCase;
        event.documentId = documentId;
        event.eventType = eventType;
        event.actor = actor.trim();
        event.reason = reason.trim();
        event.createdAt = now;
        return event;
    }

    public CurationCandidateEventType getEventType() {
        return eventType;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public String getActor() {
        return actor;
    }

    public String getReason() {
        return reason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
