package br.com.infocedro.clipper.clipper;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

// Acesso aos feedbacks de diagnóstico. A busca por ticket existe pra
// curadoria — vários feedbacks por ticket são esperados (um por rodada
// de diagnóstico marcada).
public interface DiagnosisFeedbackRepository extends JpaRepository<DiagnosisFeedback, Long> {

    List<DiagnosisFeedback> findByTicketId(Long ticketId);
}
