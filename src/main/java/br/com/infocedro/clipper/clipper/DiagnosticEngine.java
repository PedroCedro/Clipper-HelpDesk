package br.com.infocedro.clipper.clipper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import br.com.infocedro.clipper.knowledge.KnowledgeArticle;
import br.com.infocedro.clipper.knowledge.KnowledgeSearch;

// O motor SÓ orquestra. Não conhece Ticket (só o contrato DiagnosticRequest),
// não conhece o LLM (só a interface DiagnosticProvider), coordena a anonimização
// em volta da IA e agora aplica o fluxo HÍBRIDO: base curada primeiro, IA depois.
@Component
public class DiagnosticEngine {

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
        Optional<KnowledgeArticle> hit = knowledge.search(query);
        if (hit.isPresent()) {
            return grounded(hit.get()); // determinístico, ancorado, sem gastar IA
        }

        // 1) SEM BASE → cai pra IA. Anonimiza: dado sensível vira token ANTES de sair.
        Map<String, String> mapping = new HashMap<>();
        DiagnosticRequest masked = new DiagnosticRequest(
                anonymizer.mask(request.title(), mapping),
                anonymizer.mask(request.description(), mapping));

        // 2) Só o texto mascarado vai pro provider — o dado real nunca sai da rede.
        DiagnosticResult result = provider.diagnose(masked);

        // 3) Des-anonimiza a resposta e MARCA como não-ancorada (gate de grounding).
        return new DiagnosticResult(
                anonymizer.unmask(result.probableCause(), mapping),
                anonymizer.unmask(result.nextSteps(), mapping),
                result.confidence(),
                "sem-base: " + result.source());
    }

    // Resposta ancorada num artigo curado: fonte oficial, confiança plena.
    private DiagnosticResult grounded(KnowledgeArticle article) {
        String source = "ancorado: " + article.getTitle();
        if (article.getSourceUrl() != null && !article.getSourceUrl().isBlank()) {
            source = source + " (" + article.getSourceUrl() + ")";
        }
        return new DiagnosticResult(
                article.getTitle(),   // problema (ver seam: content mistura causa+passos)
                article.getContent(), // texto curado inteiro
                1.0,                  // curado oficial → sem autoavaliação do LLM
                source);
    }
}
