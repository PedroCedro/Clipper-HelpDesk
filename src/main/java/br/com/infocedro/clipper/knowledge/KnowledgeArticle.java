package br.com.infocedro.clipper.knowledge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

// Artigo simples da base de conhecimento interna usada para apoiar diagnósticos.
@Entity
public class KnowledgeArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    @Column(columnDefinition = "TEXT")
    private String content;
    private String keywords;
    private String sourceUrl;

    public KnowledgeArticle() {
    }

    public KnowledgeArticle(String title, String content) {
        this.title = title;
        this.content = content;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public Long getId() {
        return id;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }
}
