package br.com.infocedro.clipper.curation;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Borda interna que traduz HTTP para os contratos do domínio de curadoria. */
@RestController
@RequestMapping("/api/curation-cases")
public class CurationController {

    private static final String ACTOR_HEADER = "X-Actor";

    private final CurationCaseService caseService;
    private final CurationCandidateService candidateService;
    private final CurationQueryService queryService;

    public CurationController(
            CurationCaseService caseService,
            CurationCandidateService candidateService,
            CurationQueryService queryService
    ) {
        this.caseService = caseService;
        this.candidateService = candidateService;
        this.queryService = queryService;
    }

    @PostMapping
    public ResponseEntity<CurationCaseSnapshot> create(
            @RequestHeader(ACTOR_HEADER) String actor,
            @RequestBody CreateCaseRequest request
    ) {
        actor = requireActor(actor);
        CurationCaseSnapshot created = caseService.create(new CreateCurationCaseCommand(
                request.originType(), request.originReference(), actor, request.reason()));
        return ResponseEntity.created(URI.create("/api/curation-cases/" + created.id())).body(created);
    }

    @GetMapping
    public List<CurationCaseSummary> list(
            @RequestParam(name = "status", required = false) CurationStatus status) {
        return queryService.list(status);
    }

    @GetMapping("/{id}")
    public CurationCaseDetail detail(@PathVariable Long id) {
        return queryService.detail(id);
    }

    @PostMapping("/{id}/candidates")
    public CurationCandidateChangeResult addCandidate(
            @PathVariable Long id,
            @RequestHeader(ACTOR_HEADER) String actor,
            @RequestBody ChangeCandidateRequest request
    ) {
        actor = requireActor(actor);
        return candidateService.add(
                id, request.documentId(), new ChangeCurationCandidateCommand(actor, request.reason()));
    }

    @DeleteMapping("/{id}/candidates/{documentId}")
    public CurationCandidateChangeResult removeCandidate(
            @PathVariable Long id,
            @PathVariable Long documentId,
            @RequestHeader(ACTOR_HEADER) String actor,
            @RequestParam("reason") String reason
    ) {
        actor = requireActor(actor);
        return candidateService.remove(
                id, documentId, new ChangeCurationCandidateCommand(actor, reason));
    }

    @PostMapping("/{id}/transitions")
    public CurationCaseSnapshot transition(
            @PathVariable Long id,
            @RequestHeader(ACTOR_HEADER) String actor,
            @RequestBody TransitionRequest request
    ) {
        actor = requireActor(actor);
        return caseService.transition(
                id, new TransitionCurationCaseCommand(request.targetStatus(), actor, request.reason()));
    }

    private String requireActor(String actor) {
        if (actor == null || actor.isBlank()) {
            throw new InvalidCurationCaseException("Header X-Actor é obrigatório");
        }
        return actor.trim();
    }

    public record CreateCaseRequest(
            CurationOriginType originType, String originReference, String reason) {
    }

    public record ChangeCandidateRequest(Long documentId, String reason) {
    }

    public record TransitionRequest(CurationStatus targetStatus, String reason) {
    }
}
