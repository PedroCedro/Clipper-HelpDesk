package br.com.infocedro.clipper.curation;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

interface CurationCaseTransitionRepository extends JpaRepository<CurationCaseTransition, Long> {

    List<CurationCaseTransition> findByCurationCase_IdOrderByCreatedAtAscIdAsc(Long curationCaseId);
}
