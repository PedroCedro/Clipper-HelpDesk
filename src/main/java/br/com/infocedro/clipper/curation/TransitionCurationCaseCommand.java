package br.com.infocedro.clipper.curation;

/** Pedido explícito de mudança de etapa, sempre acompanhado de auditoria. */
public record TransitionCurationCaseCommand(
        CurationStatus targetStatus,
        String actor,
        String reason
) {
}
