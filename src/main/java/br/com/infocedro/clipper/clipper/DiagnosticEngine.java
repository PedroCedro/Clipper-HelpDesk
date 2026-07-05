package br.com.infocedro.clipper.clipper;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

// O motor SÓ orquestra. Não conhece Ticket (só o contrato DiagnosticRequest),
// não conhece o LLM (só a interface DiagnosticProvider) e agora coordena a
// anonimização em volta da chamada de IA.
@Component
public class DiagnosticEngine {

    private final DiagnosticProvider provider;
    private final Anonymizer anonymizer;

    public DiagnosticEngine(DiagnosticProvider provider, Anonymizer anonymizer) {
        this.provider = provider;
        this.anonymizer = anonymizer;
    }

    public DiagnosticResult run(DiagnosticRequest request) {
        // 1) Anonimiza: dado sensível vira token ANTES de sair pra IA. O mapa fica local.
        Map<String, String> mapping = new HashMap<>();
        DiagnosticRequest masked = new DiagnosticRequest(
                anonymizer.mask(request.title(), mapping),
                anonymizer.mask(request.description(), mapping));

        // 2) Só o texto mascarado vai pro provider — o dado real nunca sai da rede.
        DiagnosticResult result = provider.diagnose(masked);

        // 3) Des-anonimiza a resposta: tokens voltam a ser o dado real pro técnico.
        return new DiagnosticResult(
                anonymizer.unmask(result.probableCause(), mapping),
                anonymizer.unmask(result.nextSteps(), mapping),
                result.confidence(),
                result.source());

        // TODO (fluxo híbrido): regras determinísticas (DiagnosticRule) para casos
        // conhecidos ANTES de chamar a IA. Encaixa aqui no orquestrador.
    }
}
