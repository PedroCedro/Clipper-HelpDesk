package br.com.infocedro.clipper.clipper;

// Saída normalizada do diagnóstico, independente do provider de IA usado.
public record DiagnosticResult(
        String probableCause,
        String nextSteps,
        Double confidence,
        String source
) {
}
