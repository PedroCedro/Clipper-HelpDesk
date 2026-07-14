package br.com.infocedro.clipper.curation;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Coordena casos e histórico na mesma transação para não produzir auditoria parcial. */
@Service
public class CurationCaseService {

    private final CurationCaseRepository caseRepository;
    private final CurationCaseTransitionRepository transitionRepository;

    public CurationCaseService(
            CurationCaseRepository caseRepository,
            CurationCaseTransitionRepository transitionRepository
    ) {
        this.caseRepository = caseRepository;
        this.transitionRepository = transitionRepository;
    }

    @Transactional
    public CurationCaseSnapshot create(CreateCurationCaseCommand command) {
        validateCreation(command);
        OffsetDateTime now = OffsetDateTime.now();
        CurationCase curationCase = caseRepository.save(CurationCase.open(command, now));
        transitionRepository.save(CurationCaseTransition.record(
                curationCase,
                null,
                CurationStatus.ABERTO,
                command.actor(),
                command.reason(),
                now));
        return CurationCaseSnapshot.from(curationCase);
    }

    @Transactional
    public CurationCaseSnapshot transition(Long id, TransitionCurationCaseCommand command) {
        validateAudit(command == null ? null : command.actor(), command == null ? null : command.reason());
        if (command.targetStatus() == null) {
            throw new InvalidCurationTransitionException("Estado de destino é obrigatório");
        }

        CurationCase curationCase = caseRepository.findById(id)
                .orElseThrow(() -> new CurationCaseNotFoundException(id));
        CurationStatus previousStatus = curationCase.getStatus();
        CurationTransitionPolicy.validateRequested(previousStatus, command.targetStatus());

        OffsetDateTime now = OffsetDateTime.now();
        curationCase.moveTo(command.targetStatus(), now);
        transitionRepository.save(CurationCaseTransition.record(
                curationCase,
                previousStatus,
                command.targetStatus(),
                command.actor(),
                command.reason(),
                now));
        return CurationCaseSnapshot.from(curationCase);
    }

    private void validateCreation(CreateCurationCaseCommand command) {
        if (command == null || command.originType() == null) {
            throw new InvalidCurationCaseException("Origem é obrigatória");
        }
        validateAudit(command.actor(), command.reason());

        boolean hasReference = command.originReference() != null && !command.originReference().isBlank();
        if (command.originType() == CurationOriginType.MANUAL && hasReference) {
            throw new InvalidCurationCaseException("Caso manual não aceita referência de origem");
        }
        if (command.originType() != CurationOriginType.MANUAL && !hasReference) {
            throw new InvalidCurationCaseException("Referência de origem é obrigatória para " + command.originType());
        }
    }

    private void validateAudit(String actor, String reason) {
        if (actor == null || actor.isBlank()) {
            throw new InvalidCurationCaseException("Ator é obrigatório para auditoria");
        }
        if (reason == null || reason.isBlank()) {
            throw new InvalidCurationCaseException("Motivo é obrigatório para auditoria");
        }
    }
}
