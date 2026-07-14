package br.com.infocedro.clipper.curation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CurationCaseServiceTest {

    @Autowired
    private CurationCaseService service;

    @Autowired
    private CurationCaseRepository caseRepository;

    @Autowired
    private CurationCaseTransitionRepository transitionRepository;

    @BeforeEach
    void cleanRepositories() {
        transitionRepository.deleteAll();
        caseRepository.deleteAll();
    }

    @Test
    void abreCasoERegistraTransicaoInicial() {
        CurationCaseSnapshot created = service.create(new CreateCurationCaseCommand(
                CurationOriginType.TICKET,
                "ticket-42",
                "pedro",
                "Problema recorrente"));

        assertThat(created.status()).isEqualTo(CurationStatus.ABERTO);
        assertThat(created.originReference()).isEqualTo("ticket-42");
        assertThat(created.createdAt()).isEqualTo(created.updatedAt());

        List<CurationCaseTransition> history = transitionRepository
                .findByCurationCase_IdOrderByCreatedAtAscIdAsc(created.id());
        assertThat(history).singleElement().satisfies(transition -> {
            assertThat(transition.getFromStatus()).isNull();
            assertThat(transition.getToStatus()).isEqualTo(CurationStatus.ABERTO);
            assertThat(transition.getActor()).isEqualTo("pedro");
            assertThat(transition.getReason()).isEqualTo("Problema recorrente");
            assertThat(transition.getCreatedAt()).isNotNull();
        });
    }

    @Test
    void aceitaOrigemManualSomenteSemReferencia() {
        CurationCaseSnapshot created = service.create(new CreateCurationCaseCommand(
                CurationOriginType.MANUAL,
                null,
                "curador",
                "Mapear procedimento"));

        assertThat(created.originReference()).isNull();
        assertThatThrownBy(() -> service.create(new CreateCurationCaseCommand(
                CurationOriginType.MANUAL,
                "referência indevida",
                "curador",
                "Mapear procedimento")))
                .isInstanceOf(InvalidCurationCaseException.class)
                .hasMessageContaining("não aceita referência");
    }

    @Test
    void exigeReferenciaParaTicketEFeedback() {
        assertThatThrownBy(() -> service.create(new CreateCurationCaseCommand(
                CurationOriginType.TICKET,
                " ",
                "curador",
                "Investigar")))
                .isInstanceOf(InvalidCurationCaseException.class);

        assertThatThrownBy(() -> service.create(new CreateCurationCaseCommand(
                CurationOriginType.FEEDBACK,
                null,
                "curador",
                "Investigar")))
                .isInstanceOf(InvalidCurationCaseException.class);
    }

    @Test
    void exigeAtorEMotivoParaAuditoria() {
        assertThatThrownBy(() -> service.create(new CreateCurationCaseCommand(
                CurationOriginType.MANUAL,
                null,
                " ",
                "Investigar")))
                .isInstanceOf(InvalidCurationCaseException.class)
                .hasMessageContaining("Ator");

        assertThatThrownBy(() -> service.create(new CreateCurationCaseCommand(
                CurationOriginType.MANUAL,
                null,
                "curador",
                null)))
                .isInstanceOf(InvalidCurationCaseException.class)
                .hasMessageContaining("Motivo");
    }

    @Test
    void descartaEPreservaHistoricoDeTransicoes() {
        CurationCaseSnapshot created = openManualCase();

        CurationCaseSnapshot discarded = service.transition(created.id(), new TransitionCurationCaseCommand(
                CurationStatus.DESCARTADO,
                "revisor",
                "Conteúdo fora do escopo"));

        assertThat(discarded.status()).isEqualTo(CurationStatus.DESCARTADO);
        List<CurationCaseTransition> history = transitionRepository
                .findByCurationCase_IdOrderByCreatedAtAscIdAsc(created.id());
        assertThat(history).hasSize(2);
        assertThat(history.get(1).getFromStatus()).isEqualTo(CurationStatus.ABERTO);
        assertThat(history.get(1).getToStatus()).isEqualTo(CurationStatus.DESCARTADO);
        assertThat(history.get(1).getActor()).isEqualTo("revisor");
    }

    @Test
    void bloqueiaRascunhoRevisaoEPublicacaoNoC2() {
        CurationCaseSnapshot created = openManualCase();

        for (CurationStatus forbidden : List.of(
                CurationStatus.COM_CANDIDATOS,
                CurationStatus.RASCUNHO,
                CurationStatus.EM_REVISAO,
                CurationStatus.PUBLICADO)) {
            assertThatThrownBy(() -> service.transition(created.id(), new TransitionCurationCaseCommand(
                    forbidden,
                    "curador",
                    "Tentar avançar")))
                    .isInstanceOf(InvalidCurationTransitionException.class);
        }
        assertThat(transitionRepository
                .findByCurationCase_IdOrderByCreatedAtAscIdAsc(created.id())).hasSize(1);
    }

    @Test
    void trataDescartadoComoEstadoTerminal() {
        CurationCaseSnapshot created = openManualCase();
        service.transition(created.id(), new TransitionCurationCaseCommand(
                CurationStatus.DESCARTADO,
                "curador",
                "Sem valor para curadoria"));

        assertThatThrownBy(() -> service.transition(created.id(), new TransitionCurationCaseCommand(
                CurationStatus.DESCARTADO,
                "curador",
                "Descartar novamente")))
                .isInstanceOf(InvalidCurationTransitionException.class);
    }

    @Test
    void retornaErroDeDominioQuandoCasoNaoExiste() {
        assertThatThrownBy(() -> service.transition(999_999L, new TransitionCurationCaseCommand(
                CurationStatus.DESCARTADO,
                "curador",
                "Caso inexistente")))
                .isInstanceOf(CurationCaseNotFoundException.class);
    }

    private CurationCaseSnapshot openManualCase() {
        return service.create(new CreateCurationCaseCommand(
                CurationOriginType.MANUAL,
                null,
                "curador",
                "Criar orientação interna"));
    }
}
