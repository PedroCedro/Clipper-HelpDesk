package br.com.infocedro.clipper.clipper;

import org.springframework.stereotype.Component;

// O motor SÓ orquestra. Não conhece Ticket (só o contrato DiagnosticRequest) e
// não conhece o LLM (só a interface DiagnosticProvider).
@Component
public class DiagnosticEngine {

    private final DiagnosticProvider provider;

    public DiagnosticEngine(DiagnosticProvider provider) {
        this.provider = provider;
    }

    public DiagnosticResult run(DiagnosticRequest request) {
        // Fluxo híbrido planejado: regras determinísticas (DiagnosticRule) para casos
        // conhecidos → anonimização OBRIGATÓRIA (mascarar CNPJ/CPF/valores) → IA para o resto.
        // TODO: encaixar regras + anonimização aqui, antes de delegar ao provider.
        return provider.diagnose(request);
    }
}
