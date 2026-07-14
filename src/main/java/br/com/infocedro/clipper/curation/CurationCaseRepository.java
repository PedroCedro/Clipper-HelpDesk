package br.com.infocedro.clipper.curation;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface CurationCaseRepository extends JpaRepository<CurationCase, Long> {

    // Serializa mudanças de candidatos do mesmo caso para sustentar a
    // idempotência e a transição única do primeiro vínculo.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CurationCase c where c.id = :id")
    Optional<CurationCase> findByIdForUpdate(@Param("id") Long id);
}
