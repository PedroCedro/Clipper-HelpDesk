package br.com.infocedro.clipper.clipper;

import java.util.Optional;

import org.springframework.stereotype.Component;

// O caso de uso do diagnóstico. O DiagnosticEngine continua puro (recebe
// request, devolve result, não conhece banco); quem liga o diagnóstico à
// vida do ticket — persistir, substituir, consultar — é o agente. Divisão
// deliberada: motor = decisão, agente = ciclo de vida. O agente segue sem
// conhecer Ticket: recebe só o ID.
@Component
public class ClipperAgent {

    private final DiagnosticEngine diagnosticEngine;
    private final DiagnosisRepository diagnosisRepository;

    public ClipperAgent(DiagnosticEngine diagnosticEngine, DiagnosisRepository diagnosisRepository) {
        this.diagnosticEngine = diagnosticEngine;
        this.diagnosisRepository = diagnosisRepository;
    }

    // Diagnostica E grava. Upsert de propósito: rediagnosticar substitui o
    // diagnóstico anterior do ticket (o console mostra o atual; histórico é
    // decisão futura — ver comentário na entidade Diagnosis).
    public DiagnosticResult analyze(Long ticketId, DiagnosticRequest request) {
        DiagnosticResult result = diagnosticEngine.run(request);

        Diagnosis diagnosis = diagnosisRepository.findByTicketId(ticketId)
                .map(existing -> {
                    existing.updateFrom(result);
                    return existing;
                })
                .orElseGet(() -> Diagnosis.of(ticketId, result));
        diagnosisRepository.save(diagnosis);

        return result;
    }

    // Último diagnóstico salvo do ticket — é o que deixa o console reabrir
    // um ticket sem rediagnosticar (e sem regastar IA).
    public Optional<DiagnosticResult> lastDiagnosis(Long ticketId) {
        return diagnosisRepository.findByTicketId(ticketId).map(Diagnosis::toResult);
    }

    // Resumo pro item da fila: só estado do gate + confiança, sem carregar
    // causa/passos. É o suficiente pra tag de IA da linha.
    public Optional<DiagnosisSummary> summaryFor(Long ticketId) {
        return diagnosisRepository.findByTicketId(ticketId)
                .map(d -> new DiagnosisSummary(d.getGroundingState(), d.getConfidence()));
    }

    // Contrato enxuto da fila (estado + confiança). Vive aqui porque é o
    // agente quem o produz — a borda só repassa.
    public record DiagnosisSummary(Grounding.State state, Double confidence) {
    }
}
