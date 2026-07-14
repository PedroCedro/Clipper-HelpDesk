package br.com.infocedro.clipper.curation;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/** Centraliza a catraca para nenhuma borda inventar transições próprias. */
final class CurationTransitionPolicy {

    private static final Map<CurationStatus, Set<CurationStatus>> REQUESTED_TRANSITIONS = buildRequestedTransitions();

    private CurationTransitionPolicy() {
    }

    static void validateRequested(CurationStatus current, CurationStatus target) {
        if (target == null || !REQUESTED_TRANSITIONS.getOrDefault(current, Set.of()).contains(target)) {
            throw new InvalidCurationTransitionException(
                    "Transição de " + current + " para " + target + " não permitida nesta etapa");
        }
    }

    /** A associação do primeiro candidato é a única operação que avança o C2.2. */
    static void validateFirstCandidate(CurationStatus current) {
        if (current != CurationStatus.ABERTO) {
            throw new InvalidCurationTransitionException(
                    "Primeiro candidato só pode avançar um caso ABERTO");
        }
    }

    private static Map<CurationStatus, Set<CurationStatus>> buildRequestedTransitions() {
        Map<CurationStatus, Set<CurationStatus>> transitions = new EnumMap<>(CurationStatus.class);
        for (CurationStatus status : CurationStatus.values()) {
            if (status != CurationStatus.DESCARTADO) {
                transitions.put(status, EnumSet.of(CurationStatus.DESCARTADO));
            }
        }
        return Map.copyOf(transitions);
    }
}
