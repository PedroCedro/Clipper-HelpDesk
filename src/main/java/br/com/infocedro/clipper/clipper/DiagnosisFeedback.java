package br.com.infocedro.clipper.clipper;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;

// Feedback "diagnóstico incorreto" — a semente do flywheel de curadoria:
// é esta tabela que a curadoria vai ler pra descobrir onde a base de
// conhecimento tem buraco (sem-base errando = falta artigo; ancorado
// errando = artigo ruim ou match errado).
//
// Guarda um SNAPSHOT do diagnóstico no momento da marcação, não uma FK:
// rediagnosticar SUBSTITUI a linha de Diagnosis, então uma referência
// apontaria pra um diagnóstico que já não é o que o técnico viu. Sem
// unique no ticketId de propósito — cada rodada de diagnóstico pode ser
// marcada de novo.
@Entity
public class DiagnosisFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ticketId;

    // O que o técnico viu quando marcou: estado do gate, fonte e a causa
    // apontada. É o contexto mínimo pra curadoria julgar o erro depois.
    @Enumerated(EnumType.STRING)
    private Grounding.State groundingState;

    private String articleTitle;
    private String articleUrl;
    private String model;
    private Double confidence;

    @Column(columnDefinition = "TEXT")
    private String probableCause;

    // Opcional: o técnico pode dizer POR QUE está errado — é o dado mais
    // valioso do flywheel quando vem.
    @Column(columnDefinition = "TEXT")
    private String reason;

    private Instant createdAt;

    protected DiagnosisFeedback() {
        // exigido pelo JPA
    }

    // Fábrica a partir da entidade viva: o snapshot é tirado aqui, num
    // lugar só, pra criação nunca divergir do que estava salvo.
    public static DiagnosisFeedback of(Diagnosis diagnosis, String reason) {
        DiagnosisFeedback feedback = new DiagnosisFeedback();
        feedback.ticketId = diagnosis.getTicketId();
        DiagnosticResult result = diagnosis.toResult();
        feedback.probableCause = result.probableCause();
        feedback.confidence = result.confidence();
        Grounding grounding = result.grounding();
        feedback.groundingState = grounding != null ? grounding.state() : null;
        feedback.articleTitle = grounding != null ? grounding.articleTitle() : null;
        feedback.articleUrl = grounding != null ? grounding.articleUrl() : null;
        feedback.model = grounding != null ? grounding.model() : null;
        feedback.reason = reason;
        return feedback;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public Grounding.State getGroundingState() {
        return groundingState;
    }

    public String getArticleTitle() {
        return articleTitle;
    }

    public String getArticleUrl() {
        return articleUrl;
    }

    public String getModel() {
        return model;
    }

    public Double getConfidence() {
        return confidence;
    }

    public String getProbableCause() {
        return probableCause;
    }

    public String getReason() {
        return reason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
