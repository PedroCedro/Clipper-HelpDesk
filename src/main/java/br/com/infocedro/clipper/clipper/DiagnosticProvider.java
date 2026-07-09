package br.com.infocedro.clipper.clipper;

// A costura do motor de IA. Tudo que fala com um LLM fica ATRÁS desta interface.
// Hoje: um provider OpenAI-compatível (GPT-4o-mini). Amanhã: cascata de modelos,
// fallback, ou até IA local — sem tocar em quem orquestra (DiagnosticEngine).
public interface DiagnosticProvider {

    // O método real: chamado + material curado de apoio (RAG-lite).
    // knowledge == null significa "sem material" — o provider monta o prompt
    // só com o chamado. Cada provider decide COMO o material entra no prompt
    // (formatar prompt é responsabilidade de quem fala com o LLM, não do
    // orquestrador).
    DiagnosticResult diagnose(DiagnosticRequest request, KnowledgeContext knowledge);

    // Atalho pro caminho sem base — evita espalhar null pelos chamadores.
    default DiagnosticResult diagnose(DiagnosticRequest request) {
        return diagnose(request, null);
    }
}
