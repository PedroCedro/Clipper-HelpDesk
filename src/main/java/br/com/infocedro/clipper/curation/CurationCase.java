package br.com.infocedro.clipper.curation;

import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Unidade de trabalho que conduz evidências brutas até conhecimento revisado. */
@Entity
@Table(name = "curation_case")
public class CurationCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CurationOriginType originType;

    @Column(length = 255)
    private String originReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CurationStatus status;

    @Column(nullable = false, length = 2000)
    private String reason;

    @Column(nullable = false, length = 255)
    private String author;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    protected CurationCase() {
    }

    static CurationCase open(CreateCurationCaseCommand command, OffsetDateTime now) {
        CurationCase curationCase = new CurationCase();
        curationCase.originType = command.originType();
        curationCase.originReference = normalizeNullable(command.originReference());
        curationCase.status = CurationStatus.ABERTO;
        curationCase.reason = command.reason().trim();
        curationCase.author = command.actor().trim();
        curationCase.createdAt = now;
        curationCase.updatedAt = now;
        return curationCase;
    }

    void moveTo(CurationStatus targetStatus, OffsetDateTime now) {
        status = targetStatus;
        updatedAt = now;
    }

    private static String normalizeNullable(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public Long getId() {
        return id;
    }

    public CurationOriginType getOriginType() {
        return originType;
    }

    public String getOriginReference() {
        return originReference;
    }

    public CurationStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public String getAuthor() {
        return author;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }
}
