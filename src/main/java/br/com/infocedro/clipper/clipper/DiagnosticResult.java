package br.com.infocedro.clipper.clipper;

// Saída normalizada do diagnóstico, independente do provider de IA usado.
//
// Dois níveis de leitura da origem:
//   - grounding: estruturado, pro frontend renderizar o selo do gate;
//   - source: string legível ("ancorado: ...", "apoiado: ... · via ..."),
//     pra humano e log — derivada, nunca parseada de volta.
public record DiagnosticResult(
        String probableCause,
        String nextSteps,
        Double confidence,
        String source,
        Grounding grounding
) {

    // Construtor de conveniência pros PROVIDERS: eles preenchem o diagnóstico
    // bruto e não conhecem o gate — quem decide/carimba o grounding é o
    // DiagnosticEngine, depois.
    public DiagnosticResult(String probableCause, String nextSteps, Double confidence, String source) {
        this(probableCause, nextSteps, confidence, source, null);
    }
}
