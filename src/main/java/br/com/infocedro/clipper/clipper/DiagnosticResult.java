package br.com.infocedro.clipper.clipper;

public record DiagnosticResult(
        String probableCause,
        String nextSteps,
        Double confidence,
        String source
) {
}