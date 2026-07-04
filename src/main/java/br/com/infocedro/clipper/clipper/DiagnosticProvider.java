package br.com.infocedro.clipper.clipper;

// A costura do motor de IA. Tudo que fala com um LLM fica ATRÁS desta interface.
// Hoje: um provider OpenAI-compatível (GPT-4o-mini). Amanhã: cascata de modelos,
// fallback, ou até IA local — sem tocar em quem orquestra (DiagnosticEngine).
public interface DiagnosticProvider {

    DiagnosticResult diagnose(DiagnosticRequest request);
}
