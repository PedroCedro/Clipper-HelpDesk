package br.com.infocedro.clipper.clipper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

// Testes do ciclo de vida do diagnóstico. Travam o contrato do agente:
// diagnosticar SALVA, rediagnosticar SUBSTITUI (nunca acumula linhas), e o
// que volta do banco reconstrói o mesmo contrato que saiu do motor — é o
// que garante que reabrir um ticket mostra a verdade sem regastar IA.
class ClipperAgentTest {

    private DiagnosticEngine engine;
    private DiagnosisRepository repository;
    private DiagnosisFeedbackRepository feedbackRepository;
    private ClipperAgent agent;

    private final DiagnosticRequest request = new DiagnosticRequest("Cupom sumiu", "não sobe na 1443");

    @BeforeEach
    void setUp() {
        engine = mock(DiagnosticEngine.class);
        repository = mock(DiagnosisRepository.class);
        feedbackRepository = mock(DiagnosisFeedbackRepository.class);
        agent = new ClipperAgent(engine, repository, feedbackRepository);
    }

    @Test
    void diagnosticarSalvaLigadoAoTicket() {
        DiagnosticResult result = resultadoAncorado();
        when(engine.run(any())).thenReturn(result);
        when(repository.findByTicketId(7L)).thenReturn(Optional.empty());

        DiagnosticResult returned = agent.analyze(7L, request);

        // O resultado volta intacto pro chamador...
        assertSame(result, returned);

        // ...e o que foi salvo carrega o vínculo com o ticket e o gate
        // desmontado em colunas (o resumo da fila lê daqui).
        ArgumentCaptor<Diagnosis> saved = ArgumentCaptor.forClass(Diagnosis.class);
        verify(repository).save(saved.capture());
        assertEquals(7L, saved.getValue().getTicketId());
        assertEquals(Grounding.State.ANCORADO, saved.getValue().getGroundingState());
        assertEquals(1.0, saved.getValue().getConfidence());
    }

    @Test
    void rediagnosticarSubstituiOAnterior() {
        Diagnosis existente = Diagnosis.of(7L, resultadoAncorado());
        when(repository.findByTicketId(7L)).thenReturn(Optional.of(existente));
        when(engine.run(any())).thenReturn(resultadoSemBase());

        agent.analyze(7L, request);

        // A MESMA entidade é atualizada e salva — sem linha nova, o que
        // preserva a regra "um diagnóstico por ticket" (unique no ticketId).
        ArgumentCaptor<Diagnosis> saved = ArgumentCaptor.forClass(Diagnosis.class);
        verify(repository).save(saved.capture());
        assertSame(existente, saved.getValue());
        assertEquals(Grounding.State.SEM_BASE, saved.getValue().getGroundingState());
        assertEquals(0.5, saved.getValue().getConfidence());
    }

    @Test
    void ultimoDiagnosticoReconstroiOContrato() {
        DiagnosticResult original = resultadoAncorado();
        when(repository.findByTicketId(7L)).thenReturn(Optional.of(Diagnosis.of(7L, original)));

        Optional<DiagnosticResult> loaded = agent.lastDiagnosis(7L);

        // Ida (entidade) e volta (contrato) sem perda: é o que o console
        // renderiza ao reabrir o ticket.
        assertTrue(loaded.isPresent());
        assertEquals(original.probableCause(), loaded.get().probableCause());
        assertEquals(original.nextSteps(), loaded.get().nextSteps());
        assertEquals(original.confidence(), loaded.get().confidence());
        assertEquals(original.source(), loaded.get().source());
        assertEquals(original.grounding(), loaded.get().grounding());
    }

    @Test
    void resumoTrazSoEstadoEConfianca() {
        when(repository.findByTicketId(7L)).thenReturn(Optional.of(Diagnosis.of(7L, resultadoAncorado())));

        Optional<ClipperAgent.DiagnosisSummary> summary = agent.summaryFor(7L);

        assertTrue(summary.isPresent());
        assertEquals(Grounding.State.ANCORADO, summary.get().state());
        assertEquals(1.0, summary.get().confidence());
    }

    @Test
    void marcarIncorretoGravaSnapshotDoDiagnostico() {
        when(repository.findByTicketId(7L)).thenReturn(Optional.of(Diagnosis.of(7L, resultadoAncorado())));
        when(feedbackRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        agent.flagIncorrect(7L, "o artigo não fala desse cenário");

        // O feedback congela o que o técnico estava vendo: mesmo que um
        // rediagnóstico substitua a linha de Diagnosis depois, a curadoria
        // ainda enxerga o diagnóstico que foi marcado como errado.
        ArgumentCaptor<DiagnosisFeedback> saved = ArgumentCaptor.forClass(DiagnosisFeedback.class);
        verify(feedbackRepository).save(saved.capture());
        assertEquals(7L, saved.getValue().getTicketId());
        assertEquals(Grounding.State.ANCORADO, saved.getValue().getGroundingState());
        assertEquals("Cupom fiscal não aparece na rotina 1443", saved.getValue().getProbableCause());
        assertEquals("https://exemplo.com/1443", saved.getValue().getArticleUrl());
        assertEquals("o artigo não fala desse cenário", saved.getValue().getReason());
    }

    @Test
    void marcarIncorretoSemDiagnosticoEhConflito() {
        when(repository.findByTicketId(7L)).thenReturn(Optional.empty());

        // Sem diagnóstico não há o que marcar — e nada pode ser gravado.
        assertThrows(DiagnosisNotFoundException.class, () -> agent.flagIncorrect(7L, null));
        verifyNoInteractions(feedbackRepository);
    }

    private DiagnosticResult resultadoAncorado() {
        return new DiagnosticResult(
                "Cupom fiscal não aparece na rotina 1443",
                "Sintoma... Passos...",
                1.0,
                "ancorado: Cupom fiscal não aparece na rotina 1443 (https://exemplo.com/1443)",
                Grounding.anchored("Cupom fiscal não aparece na rotina 1443", "https://exemplo.com/1443"));
    }

    private DiagnosticResult resultadoSemBase() {
        return new DiagnosticResult(
                "causa da IA",
                "passos da IA",
                0.5,
                "sem-base: gpt-4o-mini",
                Grounding.ungrounded("gpt-4o-mini"));
    }
}
