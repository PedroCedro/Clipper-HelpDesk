package br.com.infocedro.clipper.ticket;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;

@Entity
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String description;
    private String status;

    // Prioridade como String (ALTA/MEDIA/BAIXA), no mesmo espírito do
    // status: vira enum quando surgir regra de negócio em cima (SLA,
    // ordenação automática) — antes disso seria cerimônia.
    private String priority;

    // Contexto do chamado, opcionais no intake: quem pediu e em qual
    // rotina do ERP. A fila e o meta-grid do console exibem.
    private String requester;
    private String routine;

    private Instant createdAt;

    public Ticket() {
    }

    // Carimbado pelo JPA na inserção — o "aberto há X min" da fila nasce
    // daqui, não de relógio do frontend.
    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public Ticket(Long id, String title, String description, String status) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getRequester() {
        return requester;
    }

    public void setRequester(String requester) {
        this.requester = requester;
    }

    public String getRoutine() {
        return routine;
    }

    public void setRoutine(String routine) {
        this.routine = routine;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
