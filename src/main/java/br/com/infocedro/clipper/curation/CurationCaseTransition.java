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

/** Registro imutável que explica quando, por quem e por que o estado mudou. */
@Entity
@Table(name = "curation_case_transition")
public class CurationCaseTransition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "curation_case_id", nullable = false)
    private CurationCase curationCase;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private CurationStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CurationStatus toStatus;

    @Column(nullable = false, length = 255)
    private String actor;

    @Column(nullable = false, length = 2000)
    private String reason;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    protected CurationCaseTransition() {
    }

    static CurationCaseTransition record(
            CurationCase curationCase,
            CurationStatus fromStatus,
            CurationStatus toStatus,
            String actor,
            String reason,
            OffsetDateTime now
    ) {
        CurationCaseTransition transition = new CurationCaseTransition();
        transition.curationCase = curationCase;
        transition.fromStatus = fromStatus;
        transition.toStatus = toStatus;
        transition.actor = actor.trim();
        transition.reason = reason.trim();
        transition.createdAt = now;
        return transition;
    }

    public Long getId() {
        return id;
    }

    public Long getCurationCaseId() {
        return curationCase.getId();
    }

    public CurationStatus getFromStatus() {
        return fromStatus;
    }

    public CurationStatus getToStatus() {
        return toStatus;
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
