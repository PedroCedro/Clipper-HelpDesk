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
import jakarta.persistence.PreUpdate;

// Diagnóstico PERSISTIDO de um ticket. Vive no módulo clipper (quem produz
// diagnóstico é o motor), mas referencia o ticket só por ID — nada de
// @ManyToOne cruzando fronteira de módulo, é isso que mantém o split
// futuro barato.
//
// UMA linha por ticket, de propósito: rediagnosticar SUBSTITUI (o console
// mostra "o" diagnóstico atual, não uma linha do tempo). Histórico de
// diagnósticos é decisão futura — quando precisar, esta unique constraint
// cai e entra um campo de versão/data no lugar.
@Entity
public class Diagnosis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long ticketId;

    // TEXT desde o nascimento: causa/passos passam fácil dos 255 do VARCHAR
    // default (lição aprendida com o content do KnowledgeArticle — e o
    // ddl-auto=update NÃO alarga coluna depois).
    @Column(columnDefinition = "TEXT")
    private String probableCause;

    @Column(columnDefinition = "TEXT")
    private String nextSteps;

    private Double confidence;

    // A string legível ("ancorado: ...") — humano/log, nunca parseada.
    private String source;

    // O gate desmontado em colunas: é o que o GET /tickets consulta pra
    // fila mostrar a tag de IA sem carregar o diagnóstico inteiro.
    @Enumerated(EnumType.STRING)
    private Grounding.State groundingState;

    private String articleTitle;
    private String articleUrl;
    private String model;

    private Instant updatedAt;

    protected Diagnosis() {
        // exigido pelo JPA
    }

    private Diagnosis(Long ticketId) {
        this.ticketId = ticketId;
    }

    // Fábrica + atualização concentram o mapeamento DiagnosticResult →
    // entidade num lugar só (criar e substituir usam o MESMO preenchimento,
    // impossível divergirem).
    public static Diagnosis of(Long ticketId, DiagnosticResult result) {
        Diagnosis diagnosis = new Diagnosis(ticketId);
        diagnosis.updateFrom(result);
        return diagnosis;
    }

    public void updateFrom(DiagnosticResult result) {
        this.probableCause = result.probableCause();
        this.nextSteps = result.nextSteps();
        this.confidence = result.confidence();
        this.source = result.source();
        Grounding grounding = result.grounding();
        this.groundingState = grounding != null ? grounding.state() : null;
        this.articleTitle = grounding != null ? grounding.articleTitle() : null;
        this.articleUrl = grounding != null ? grounding.articleUrl() : null;
        this.model = grounding != null ? grounding.model() : null;
    }

    // Caminho de volta: reconstrói o contrato que a API devolve. A entidade
    // é detalhe de persistência; quem sai pela borda é o DiagnosticResult.
    public DiagnosticResult toResult() {
        Grounding grounding = groundingState != null
                ? new Grounding(groundingState, articleTitle, articleUrl, model)
                : null;
        return new DiagnosticResult(probableCause, nextSteps, confidence, source, grounding);
    }

    @PrePersist
    @PreUpdate
    void touch() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public Double getConfidence() {
        return confidence;
    }

    public Grounding.State getGroundingState() {
        return groundingState;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
