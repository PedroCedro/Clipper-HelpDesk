package br.com.infocedro.clipper.knowledge;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;

// Retrieval: dado o texto de um ticket, acha o artigo curado mais relevante.
// Busca por palavra-chave (antes de embeddings). Vive FORA do motor pra ele só
// orquestrar — uma responsabilidade por classe.
@Service
public class KnowledgeSearch {

    // Quantos tokens distintos do ticket precisam casar com o MESMO artigo pra
    // o match contar como FORTE (lookup puro, artigo verbatim). Abaixo disso,
    // 1 token vira match FRACO (artigo vai de apoio pro prompt da IA — RAG):
    // sozinho ele não sustenta o selo "ancorado", mas jogar fora seria
    // desperdiçar a pista. É o botão que separa lookup de RAG.
    private static final int MIN_TOKEN_HITS = 2;

    private final KnowledgeRepository repository;

    public KnowledgeSearch(KnowledgeRepository repository) {
        this.repository = repository;
    }

    public Optional<KnowledgeMatch> search(String query) {
        if (query == null || query.isBlank()) {
            return Optional.empty();
        }

        // Conta, por artigo, quantos tokens distintos do ticket batem nas keywords.
        Map<Long, Integer> hitsByArticle = new HashMap<>();
        Map<Long, KnowledgeArticle> articlesById = new HashMap<>();

        for (String token : tokenize(query)) {
            // O banco é só o filtro GROSSO (substring): "144" também traria o
            // artigo de "1443". Quem decide se o ponto vale é a confirmação
            // por palavra inteira logo abaixo — errar aqui entregaria ao
            // técnico a solução do problema errado com selo de "ancorado".
            List<KnowledgeArticle> matches = repository.findByKeywordsContainingIgnoreCase(token);
            for (KnowledgeArticle article : matches) {
                if (!tokenize(article.getKeywords()).contains(token)) {
                    continue; // casou só por pedaço de keyword → não conta
                }
                hitsByArticle.merge(article.getId(), 1, Integer::sum);
                articlesById.put(article.getId(), article);
            }
        }

        // Vence o artigo com mais tokens casados; o limiar não descarta mais,
        // ele CLASSIFICA: no limiar ou acima → forte; abaixo (1 token) → fraco.
        // Empate no placar é decidido arbitrariamente — ok pro MVP, e no fraco
        // a IA ainda tempera o artigo escolhido com o contexto do ticket.
        return hitsByArticle.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> new KnowledgeMatch(
                        articlesById.get(entry.getKey()),
                        entry.getValue() >= MIN_TOKEN_HITS
                                ? KnowledgeMatch.Strength.STRONG
                                : KnowledgeMatch.Strength.WEAK));
    }

    // "Feio primeiro": minúsculas, quebra em palavras, descarta tokens curtos.
    // Ainda SEM stopwords / normalização de acento — refina quando um teste quebrar.
    private Set<String> tokenize(String text) {
        Set<String> tokens = new HashSet<>();
        for (String raw : text.toLowerCase().split("[^\\p{L}\\p{N}]+")) {
            if (raw.length() >= 3) {
                tokens.add(raw);
            }
        }
        return tokens;
    }
}
