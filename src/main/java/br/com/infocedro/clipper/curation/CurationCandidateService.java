package br.com.infocedro.clipper.curation;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Mantém vínculo ativo, evento histórico e estado do caso atomicamente coerentes. */
@Service
public class CurationCandidateService {

    private final CurationCaseRepository caseRepository;
    private final CurationCaseTransitionRepository transitionRepository;
    private final CurationCandidateRepository candidateRepository;
    private final CurationCandidateEventRepository eventRepository;
    private final CurationCatalogPort catalogPort;

    public CurationCandidateService(
            CurationCaseRepository caseRepository,
            CurationCaseTransitionRepository transitionRepository,
            CurationCandidateRepository candidateRepository,
            CurationCandidateEventRepository eventRepository,
            CurationCatalogPort catalogPort
    ) {
        this.caseRepository = caseRepository;
        this.transitionRepository = transitionRepository;
        this.candidateRepository = candidateRepository;
        this.eventRepository = eventRepository;
        this.catalogPort = catalogPort;
    }

    @Transactional
    public CurationCandidateChangeResult add(
            Long caseId, Long documentId, ChangeCurationCandidateCommand command) {
        validate(caseId, documentId, command);
        CurationCase curationCase = findCaseForUpdate(caseId);
        validateCaseAcceptsCandidates(curationCase);
        var existing = candidateRepository.findByCurationCase_IdAndDocumentId(caseId, documentId);
        if (existing.isPresent()) {
            return result(curationCase, documentId, false);
        }
        if (!catalogPort.exists(documentId)) {
            throw new CurationDocumentNotFoundException(documentId);
        }

        OffsetDateTime now = OffsetDateTime.now();
        candidateRepository.save(CurationCandidate.add(
                curationCase, documentId, command.actor(), command.reason(), now));
        eventRepository.save(CurationCandidateEvent.record(
                curationCase, documentId, CurationCandidateEventType.ADDED,
                command.actor(), command.reason(), now));

        if (curationCase.getStatus() == CurationStatus.ABERTO) {
            CurationTransitionPolicy.validateFirstCandidate(curationCase.getStatus());
            curationCase.moveTo(CurationStatus.COM_CANDIDATOS, now);
            transitionRepository.save(CurationCaseTransition.record(
                    curationCase,
                    CurationStatus.ABERTO,
                    CurationStatus.COM_CANDIDATOS,
                    command.actor(),
                    command.reason(),
                    now));
        } else {
            curationCase.touch(now);
        }
        return result(curationCase, documentId, true);
    }

    @Transactional
    public CurationCandidateChangeResult remove(
            Long caseId, Long documentId, ChangeCurationCandidateCommand command) {
        validate(caseId, documentId, command);
        CurationCase curationCase = findCaseForUpdate(caseId);
        validateCaseAcceptsCandidates(curationCase);
        var existing = candidateRepository.findByCurationCase_IdAndDocumentId(caseId, documentId);
        if (existing.isEmpty()) {
            return result(curationCase, documentId, false);
        }

        OffsetDateTime now = OffsetDateTime.now();
        candidateRepository.delete(existing.get());
        curationCase.touch(now);
        eventRepository.save(CurationCandidateEvent.record(
                curationCase, documentId, CurationCandidateEventType.REMOVED,
                command.actor(), command.reason(), now));
        return result(curationCase, documentId, true);
    }

    private CurationCase findCaseForUpdate(Long caseId) {
        return caseRepository.findByIdForUpdate(caseId)
                .orElseThrow(() -> new CurationCaseNotFoundException(caseId));
    }

    private void validateCaseAcceptsCandidates(CurationCase curationCase) {
        if (curationCase.getStatus() != CurationStatus.ABERTO
                && curationCase.getStatus() != CurationStatus.COM_CANDIDATOS) {
            throw new InvalidCurationTransitionException(
                    "Caso em " + curationCase.getStatus() + " não aceita alteração de candidatos");
        }
    }

    private void validate(Long caseId, Long documentId, ChangeCurationCandidateCommand command) {
        if (caseId == null || documentId == null) {
            throw new InvalidCurationCaseException("Caso e documento são obrigatórios");
        }
        if (command == null || command.actor() == null || command.actor().isBlank()) {
            throw new InvalidCurationCaseException("Ator é obrigatório para auditoria");
        }
        if (command.reason() == null || command.reason().isBlank()) {
            throw new InvalidCurationCaseException("Motivo é obrigatório para auditoria");
        }
    }

    private CurationCandidateChangeResult result(
            CurationCase curationCase, Long documentId, boolean changed) {
        return new CurationCandidateChangeResult(
                curationCase.getId(),
                documentId,
                changed,
                candidateRepository.countByCurationCase_Id(curationCase.getId()),
                curationCase.getStatus());
    }
}
