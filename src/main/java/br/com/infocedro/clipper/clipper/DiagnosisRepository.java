package br.com.infocedro.clipper.clipper;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

// Acesso ao diagnóstico persistido. Como é uma linha por ticket, a busca
// por ticketId devolve no máximo um.
public interface DiagnosisRepository extends JpaRepository<Diagnosis, Long> {

    Optional<Diagnosis> findByTicketId(Long ticketId);
}
