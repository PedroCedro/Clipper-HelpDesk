package br.com.infocedro.clipper.curation;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

interface CurationCandidateRepository extends JpaRepository<CurationCandidate, Long> {

    Optional<CurationCandidate> findByCurationCase_IdAndDocumentId(Long caseId, Long documentId);

    long countByCurationCase_Id(Long caseId);
}
