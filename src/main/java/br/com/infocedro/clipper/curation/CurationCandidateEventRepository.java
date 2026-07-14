package br.com.infocedro.clipper.curation;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface CurationCandidateEventRepository extends JpaRepository<CurationCandidateEvent, Long> {

    List<CurationCandidateEvent> findByCurationCase_IdOrderByCreatedAtAscIdAsc(Long caseId);
}
