package br.com.infocedro.clipper.curation;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Monta leituras próprias para a borda sem devolver entidades persistentes. */
@Service
public class CurationQueryService {

    private final CurationCaseRepository caseRepository;
    private final CurationCandidateRepository candidateRepository;
    private final CurationCaseTransitionRepository transitionRepository;
    private final CurationCandidateEventRepository eventRepository;
    private final CurationCatalogPort catalogPort;

    public CurationQueryService(
            CurationCaseRepository caseRepository,
            CurationCandidateRepository candidateRepository,
            CurationCaseTransitionRepository transitionRepository,
            CurationCandidateEventRepository eventRepository,
            CurationCatalogPort catalogPort
    ) {
        this.caseRepository = caseRepository;
        this.candidateRepository = candidateRepository;
        this.transitionRepository = transitionRepository;
        this.eventRepository = eventRepository;
        this.catalogPort = catalogPort;
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
        var activeCandidates = candidateRepository.findByCurationCase_IdOrderByCreatedAtAscIdAsc(id);
        Map<Long, CurationCatalogDocument> documents = activeCandidates.isEmpty()
                ? Map.of()
                : catalogPort.findSummaries(
                                activeCandidates.stream().map(CurationCandidate::getDocumentId).toList()).stream()
                        .collect(Collectors.toMap(CurationCatalogDocument::id, Function.identity()));
        List<CurationCandidateView> candidates = activeCandidates.stream()
                .map(candidate -> CurationCandidateView.from(candidate, requireDocument(documents, candidate)))
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

    private CurationCatalogDocument requireDocument(
            Map<Long, CurationCatalogDocument> documents, CurationCandidate candidate) {
        CurationCatalogDocument document = documents.get(candidate.getDocumentId());
        if (document == null) {
            throw new CurationDocumentNotFoundException(candidate.getDocumentId());
        }
        return document;
    }

    private CurationCaseSummary summary(CurationCase curationCase) {
        return new CurationCaseSummary(
                curationCase.getId(), curationCase.getOriginType(), curationCase.getOriginReference(),
                curationCase.getStatus(), curationCase.getReason(), curationCase.getAuthor(),
                candidateRepository.countByCurationCase_Id(curationCase.getId()),
                curationCase.getCreatedAt(), curationCase.getUpdatedAt());
    }
}
