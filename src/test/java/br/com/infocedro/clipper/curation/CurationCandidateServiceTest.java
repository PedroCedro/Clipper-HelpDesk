package br.com.infocedro.clipper.curation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CurationCandidateServiceTest {

    @Autowired
    private CurationCandidateService candidateService;
    @Autowired
    private CurationCaseService caseService;
    @Autowired
    private CurationQueryService queryService;
    @Autowired
    private CurationCaseRepository caseRepository;
    @Autowired
    private CurationCaseTransitionRepository transitionRepository;
    @Autowired
    private CurationCandidateRepository candidateRepository;
    @Autowired
    private CurationCandidateEventRepository eventRepository;

    @MockitoBean
    private CurationCatalogPort catalogPort;

    @BeforeEach
    void limpaRepositorios() {
        eventRepository.deleteAll();
        candidateRepository.deleteAll();
        transitionRepository.deleteAll();
        caseRepository.deleteAll();
    }

    @Test
    void associaPrimeiroCandidatoEAvancaEstado() {
        CurationCaseSnapshot curationCase = abreCaso();
        when(catalogPort.exists(101L)).thenReturn(true);

        CurationCandidateChangeResult result = candidateService.add(
                curationCase.id(), 101L, comando("Fonte relevante"));

        assertThat(result.changed()).isTrue();
        assertThat(result.candidateCount()).isEqualTo(1);
        assertThat(result.caseStatus()).isEqualTo(CurationStatus.COM_CANDIDATOS);
        assertThat(candidateRepository.findByCurationCase_IdAndDocumentId(curationCase.id(), 101L)).isPresent();
        assertThat(historicoDoCaso(curationCase.id()))
                .extracting(CurationCaseTransition::getToStatus)
                .containsExactly(CurationStatus.ABERTO, CurationStatus.COM_CANDIDATOS);
        assertThat(eventosDoCaso(curationCase.id())).singleElement().satisfies(event -> {
            assertThat(event.getEventType()).isEqualTo(CurationCandidateEventType.ADDED);
            assertThat(event.getDocumentId()).isEqualTo(101L);
            assertThat(event.getActor()).isEqualTo("curador");
        });
    }

    @Test
    void associacaoRepetidaEIdempotente() {
        CurationCaseSnapshot curationCase = abreCaso();
        when(catalogPort.exists(101L)).thenReturn(true);
        candidateService.add(curationCase.id(), 101L, comando("Primeira associação"));

        CurationCandidateChangeResult repeated = candidateService.add(
                curationCase.id(), 101L, comando("Tentativa repetida"));

        assertThat(repeated.changed()).isFalse();
        assertThat(repeated.candidateCount()).isEqualTo(1);
        assertThat(eventosDoCaso(curationCase.id())).hasSize(1);
        assertThat(historicoDoCaso(curationCase.id())).hasSize(2);
        verify(catalogPort).exists(101L);
    }

    @Test
    void segundoCandidatoNaoRepeteTransicaoDeEstado() {
        CurationCaseSnapshot curationCase = abreCaso();
        when(catalogPort.exists(101L)).thenReturn(true);
        when(catalogPort.exists(102L)).thenReturn(true);
        candidateService.add(curationCase.id(), 101L, comando("Primeira fonte"));

        CurationCandidateChangeResult result = candidateService.add(
                curationCase.id(), 102L, comando("Fonte complementar"));

        assertThat(result.candidateCount()).isEqualTo(2);
        assertThat(historicoDoCaso(curationCase.id())).hasSize(2);
        assertThat(eventosDoCaso(curationCase.id()))
                .extracting(CurationCandidateEvent::getEventType)
                .containsExactly(CurationCandidateEventType.ADDED, CurationCandidateEventType.ADDED);
    }

    @Test
    void removeVinculoMasPreservaEventosEEstadoAlcancado() {
        CurationCaseSnapshot curationCase = abreCaso();
        when(catalogPort.exists(101L)).thenReturn(true);
        candidateService.add(curationCase.id(), 101L, comando("Associar"));

        CurationCandidateChangeResult removed = candidateService.remove(
                curationCase.id(), 101L, comando("Fonte insuficiente"));

        assertThat(removed.changed()).isTrue();
        assertThat(removed.candidateCount()).isZero();
        assertThat(removed.caseStatus()).isEqualTo(CurationStatus.COM_CANDIDATOS);
        assertThat(candidateRepository.findByCurationCase_IdAndDocumentId(curationCase.id(), 101L)).isEmpty();
        assertThat(eventosDoCaso(curationCase.id()))
                .extracting(CurationCandidateEvent::getEventType)
                .containsExactly(CurationCandidateEventType.ADDED, CurationCandidateEventType.REMOVED);
    }

    @Test
    void remocaoRepetidaEIdempotente() {
        CurationCaseSnapshot curationCase = abreCaso();

        CurationCandidateChangeResult result = candidateService.remove(
                curationCase.id(), 999L, comando("Nada para remover"));

        assertThat(result.changed()).isFalse();
        assertThat(result.candidateCount()).isZero();
        assertThat(eventosDoCaso(curationCase.id())).isEmpty();
    }

    @Test
    void reassociacaoCriaNovoEventoSemRecuarOEstado() {
        CurationCaseSnapshot curationCase = abreCaso();
        when(catalogPort.exists(101L)).thenReturn(true);
        candidateService.add(curationCase.id(), 101L, comando("Associar"));
        candidateService.remove(curationCase.id(), 101L, comando("Remover"));

        CurationCandidateChangeResult reassociated = candidateService.add(
                curationCase.id(), 101L, comando("Reavaliado como relevante"));

        assertThat(reassociated.changed()).isTrue();
        assertThat(reassociated.caseStatus()).isEqualTo(CurationStatus.COM_CANDIDATOS);
        assertThat(eventosDoCaso(curationCase.id()))
                .extracting(CurationCandidateEvent::getEventType)
                .containsExactly(
                        CurationCandidateEventType.ADDED,
                        CurationCandidateEventType.REMOVED,
                        CurationCandidateEventType.ADDED);
        assertThat(historicoDoCaso(curationCase.id())).hasSize(2);
    }

    @Test
    void rejeitaDocumentoAusenteSemAlterarCaso() {
        CurationCaseSnapshot curationCase = abreCaso();
        when(catalogPort.exists(404L)).thenReturn(false);

        assertThatThrownBy(() -> candidateService.add(
                curationCase.id(), 404L, comando("Documento inválido")))
                .isInstanceOf(CurationDocumentNotFoundException.class);

        assertThat(candidateRepository.countByCurationCase_Id(curationCase.id())).isZero();
        assertThat(eventosDoCaso(curationCase.id())).isEmpty();
        assertThat(historicoDoCaso(curationCase.id())).hasSize(1);
        assertThat(caseRepository.findById(curationCase.id()).orElseThrow().getStatus())
                .isEqualTo(CurationStatus.ABERTO);
    }

    @Test
    void rejeitaAlteracaoDeCandidatosEmCasoDescartado() {
        CurationCaseSnapshot curationCase = abreCaso();
        caseService.transition(curationCase.id(), new TransitionCurationCaseCommand(
                CurationStatus.DESCARTADO, "curador", "Descartar caso"));

        assertThatThrownBy(() -> candidateService.add(
                curationCase.id(), 101L, comando("Não pode associar")))
                .isInstanceOf(InvalidCurationTransitionException.class);
    }

    @Test
    void consultaDetalheComContagemVinculosEHistoricosOrdenados() {
        CurationCaseSnapshot curationCase = abreCaso();
        when(catalogPort.exists(101L)).thenReturn(true);
        candidateService.add(curationCase.id(), 101L, comando("Associar"));
        candidateService.remove(curationCase.id(), 101L, comando("Remover"));

        CurationCaseDetail detail = queryService.detail(curationCase.id());

        assertThat(detail.candidateCount()).isZero();
        assertThat(detail.candidates()).isEmpty();
        assertThat(detail.transitions())
                .extracting(CurationTransitionView::toStatus)
                .containsExactly(CurationStatus.ABERTO, CurationStatus.COM_CANDIDATOS);
        assertThat(detail.candidateEvents())
                .extracting(CurationCandidateEventView::eventType)
                .containsExactly(CurationCandidateEventType.ADDED, CurationCandidateEventType.REMOVED);
        assertThat(queryService.list(CurationStatus.COM_CANDIDATOS))
                .singleElement()
                .extracting(CurationCaseSummary::candidateCount)
                .isEqualTo(0L);
    }

    @Test
    void consultaCandidatosAtivosEmUmUnicoLoteLeve() {
        CurationCaseSnapshot curationCase = abreCaso();
        when(catalogPort.exists(101L)).thenReturn(true);
        when(catalogPort.exists(102L)).thenReturn(true);
        candidateService.add(curationCase.id(), 101L, comando("Primeira fonte"));
        candidateService.add(curationCase.id(), 102L, comando("Segunda fonte"));
        when(catalogPort.findSummaries(List.of(101L, 102L))).thenReturn(List.of(
                new CurationCatalogDocument(101L, "Fonte A", "fiscal", "Fonte oficial", "https://fonte/a"),
                new CurationCatalogDocument(102L, "Fonte B", "vendas", "Fonte oficial", "https://fonte/b")));

        CurationCaseDetail detail = queryService.detail(curationCase.id());

        assertThat(detail.candidates()).extracting(CurationCandidateView::title)
                .containsExactly("Fonte A", "Fonte B");
        verify(catalogPort, times(1)).findSummaries(List.of(101L, 102L));
    }

    private CurationCaseSnapshot abreCaso() {
        return caseService.create(new CreateCurationCaseCommand(
                CurationOriginType.MANUAL, null, "curador", "Investigar conhecimento"));
    }

    private ChangeCurationCandidateCommand comando(String reason) {
        return new ChangeCurationCandidateCommand("curador", reason);
    }

    private List<CurationCaseTransition> historicoDoCaso(Long caseId) {
        return transitionRepository.findByCurationCase_IdOrderByCreatedAtAscIdAsc(caseId);
    }

    private List<CurationCandidateEvent> eventosDoCaso(Long caseId) {
        return eventRepository.findByCurationCase_IdOrderByCreatedAtAscIdAsc(caseId);
    }
}
