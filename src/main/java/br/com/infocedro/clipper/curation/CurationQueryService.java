package br.com.infocedro.clipper.curation;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Monta leituras próprias para a borda sem devolver entidades persistentes. */
@Service
public class CurationQueryService {

    private final CurationCaseRepository caseRepository;
    private final CurationCandidateRepository candidateRepository;
    private final CurationCaseTransitionRepository transitionRepository;
    private final CurationCandidateEventRepository eventRepository;

    public CurationQueryService(
            CurationCaseRepository caseRepository,
            CurationCandidateRepository candidateRepository,
            CurationCaseTransitionRepository transitionRepository,
            CurationCandidateEventRepository eventRepository
    ) {
        this.caseRepository = caseRepository;
        this.candidateRepository = candidateRepository;
        this.transitionRepository = transitionRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional(readOnly = true)
    public List<CurationCaseSummary> list(CurationStatus status) {
        List<CurationCase> cases = status == null
                ? caseRepository.findAllByOrderByUpdatedAtDescIdDesc()
                : caseRepository.findByStatusOrderByUpdatedAtDescIdDesc(status);
        return cases.stream().map(this::summary).toList();
    }

    @Transactional(readOnly = true)
    public CurationCaseDetail detail(Long id) {
        CurationCase curationCase = caseRepository.findById(id)
                .orElseThrow(() -> new CurationCaseNotFoundException(id));
        List<CurationCandidateView> candidates = candidateRepository
                .findByCurationCase_IdOrderByCreatedAtAscIdAsc(id).stream()
                .map(CurationCandidateView::from)
                .toList();
        List<CurationTransitionView> transitions = transitionRepository
                .findByCurationCase_IdOrderByCreatedAtAscIdAsc(id).stream()
                .map(CurationTransitionView::from)
                .toList();
        List<CurationCandidateEventView> events = eventRepository
                .findByCurationCase_IdOrderByCreatedAtAscIdAsc(id).stream()
                .map(CurationCandidateEventView::from)
                .toList();
        return new CurationCaseDetail(
                curationCase.getId(), curationCase.getOriginType(), curationCase.getOriginReference(),
                curationCase.getStatus(), curationCase.getReason(), curationCase.getAuthor(),
                candidates.size(), curationCase.getCreatedAt(), curationCase.getUpdatedAt(),
                candidates, transitions, events);
    }

    private CurationCaseSummary summary(CurationCase curationCase) {
        return new CurationCaseSummary(
                curationCase.getId(), curationCase.getOriginType(), curationCase.getOriginReference(),
                curationCase.getStatus(), curationCase.getReason(), curationCase.getAuthor(),
                candidateRepository.countByCurationCase_Id(curationCase.getId()),
                curationCase.getCreatedAt(), curationCase.getUpdatedAt());
    }
}
