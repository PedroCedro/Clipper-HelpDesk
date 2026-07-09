package br.com.infocedro.clipper.clipper;

// Material curado que acompanha um chamado até o provider de IA (RAG-lite).
//
// É um contrato DESTE módulo de propósito: o provider não pode conhecer a
// entidade JPA KnowledgeArticle (módulo knowledge) — fronteira de módulo se
// cruza por contrato, não por entidade. Quem traduz entidade → contrato é o
// orquestrador (DiagnosticEngine).
//
// Conteúdo curado é público/genérico (docs oficiais), então NÃO passa pela
// anonimização — só o texto do chamado é mascarado.
public record KnowledgeContext(String title, String content, String sourceUrl) {
}
