package br.com.infocedro.clipper.clipper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import br.com.infocedro.clipper.knowledge.KnowledgeArticle;
import br.com.infocedro.clipper.knowledge.KnowledgeMatch;
import br.com.infocedro.clipper.knowledge.KnowledgeSearch;

// O motor SÓ orquestra. Não conhece Ticket (só o contrato DiagnosticRequest),
// não conhece o LLM (só a interface DiagnosticProvider), coordena a anonimização
// em volta da IA e decide o destino do chamado pelo gate de grounding:
//
//   match FORTE  → "ancorado: ..."  artigo curado verbatim, sem IA;
//   match FRACO  → "apoiado: ..."   IA costura o artigo ao caso (RAG-lite);
//   sem match    → "sem-base: ..."  IA sozinha, sem lastro na base.
//
// O prefixo do source É o contrato que o frontend consome — mudou aqui,
// quebra o DiagnosticEngineTest antes de quebrar a tela.
@Component
public class DiagnosticEngine {

    // Tetos de confiança por estado do gate (Degrau 3). A confiança da IA é
    // autoavaliação — sem lastro, o modelo pode dizer 0.9 na maior cara de pau.
    // O teto é a régua de honestidade do produto: apoiado fica logo abaixo do
    // ancorado (1.0), e sem-base nunca passa de "talvez". É TETO, não piso:
    // se o modelo disser 0.3, vale o 0.3 dele.
    private static final double MAX_CONFIDENCE_APOIADO = 0.9;
    private static final double MAX_CONFIDENCE_SEM_BASE = 0.5;

    private final DiagnosticProvider provider;
    private final Anonymizer anonymizer;
    private final KnowledgeSearch knowledge;

    public DiagnosticEngine(DiagnosticProvider provider, Anonymizer anonymizer, KnowledgeSearch knowledge) {
        this.provider = provider;
        this.anonymizer = anonymizer;
        this.knowledge = knowledge;
    }

    public DiagnosticResult run(DiagnosticRequest request) {
        // 0) LOOKUP (local): busca na base com o texto REAL do ticket. Nada sai da
        //    rede aqui, então NÃO anonimiza — e o texto real preserva as âncoras de
        //    busca (ex.: "1443", "NFC-e") que a máscara destruiria.
        String query = request.title() + " " + request.description();
        Optional<KnowledgeMatch> match = knowledge.search(query);
        if (match.isPresent() && match.get().isStrong()) {
            return grounded(match.get().article()); // determinístico, sem gastar IA
        }

        // 1) A IA vai entrar em cena (com ou sem apoio) → anonimiza ANTES:
        //    dado sensível vira token e o mapa fica local. Só o texto mascarado
        //    cruza a fronteira — o dado real nunca sai da rede.
        Map<String, String> mapping = new HashMap<>();
        DiagnosticRequest masked = new DiagnosticRequest(
                anonymizer.mask(request.title(), mapping),
                anonymizer.mask(request.description(), mapping));

        // 2) Match FRACO → RAG-lite: o artigo vai de material de apoio no prompt.
        //    A entidade não cruza a fronteira de módulo — vira KnowledgeContext.
        //    O artigo é doc pública/genérica, então segue sem mascarar.
        if (match.isPresent()) {
            KnowledgeArticle article = match.get().article();
            DiagnosticResult result = provider.diagnose(masked, contextOf(article));
            return unmasked(result, mapping,
                    "apoiado: " + titleWithUrl(article) + " · via " + result.source(),
                    MAX_CONFIDENCE_APOIADO,
                    Grounding.supported(article.getTitle(), article.getSourceUrl(), result.source()));
        }

        // 3) Sem pista na base → IA sozinha, marcada como sem lastro.
        DiagnosticResult result = provider.diagnose(masked);
        return unmasked(result, mapping, "sem-base: " + result.source(),
                MAX_CONFIDENCE_SEM_BASE, Grounding.ungrounded(result.source()));
    }

    // Resposta ancorada num artigo curado: fonte oficial, confiança plena.
    private DiagnosticResult grounded(KnowledgeArticle article) {
        return new DiagnosticResult(
                article.getTitle(),   // problema (ver seam: content mistura causa+passos)
                article.getContent(), // texto curado inteiro
                1.0,                  // curado oficial → sem autoavaliação do LLM
                "ancorado: " + titleWithUrl(article),
                Grounding.anchored(article.getTitle(), article.getSourceUrl()));
    }

    // Des-anonimiza a resposta da IA (tokens voltam a ser o dado real),
    // carimba o gate (string legível + grounding estruturado) e aplica o teto
    // de confiança do estado — a autoavaliação do LLM nunca supera o lastro.
    private DiagnosticResult unmasked(DiagnosticResult result, Map<String, String> mapping,
            String source, double confidenceCeiling, Grounding grounding) {
        return new DiagnosticResult(
                anonymizer.unmask(result.probableCause(), mapping),
                anonymizer.unmask(result.nextSteps(), mapping),
                Math.min(result.confidence(), confidenceCeiling),
                source,
                grounding);
    }

    // Tradução entidade → contrato: o provider não conhece o módulo knowledge.
    private KnowledgeContext contextOf(KnowledgeArticle article) {
        return new KnowledgeContext(article.getTitle(), article.getContent(), article.getSourceUrl());
    }

    // Rótulo rastreável do artigo pro campo source: título e, se houver, a URL
    // oficial — é o que deixa o técnico conferir a fonte com um clique.
    private String titleWithUrl(KnowledgeArticle article) {
        String label = article.getTitle();
        if (article.getSourceUrl() != null && !article.getSourceUrl().isBlank()) {
            label = label + " (" + article.getSourceUrl() + ")";
        }
        return label;
    }
}
