package br.com.infocedro.clipper.curation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class CurationTransitionPolicyTest {

    @Test
    void permiteDescartarDeTodaEtapaAtiva() {
        for (CurationStatus current : CurationStatus.values()) {
            if (current != CurationStatus.DESCARTADO) {
                assertThatCode(() -> CurationTransitionPolicy.validateRequested(
                        current,
                        CurationStatus.DESCARTADO)).doesNotThrowAnyException();
            }
        }
    }

    @Test
    void reservaAvancoDeCandidatoParaOperacaoDeAssociacao() {
        assertThatCode(() -> CurationTransitionPolicy.validateFirstCandidate(CurationStatus.ABERTO))
                .doesNotThrowAnyException();

        assertThatThrownBy(() -> CurationTransitionPolicy.validateFirstCandidate(CurationStatus.COM_CANDIDATOS))
                .isInstanceOf(InvalidCurationTransitionException.class);
        assertThatThrownBy(() -> CurationTransitionPolicy.validateRequested(
                CurationStatus.ABERTO,
                CurationStatus.COM_CANDIDATOS))
                .isInstanceOf(InvalidCurationTransitionException.class);
    }

    @Test
    void rejeitaPublicacaoIncondicionalmenteNoC2() {
        for (CurationStatus current : CurationStatus.values()) {
            assertThatThrownBy(() -> CurationTransitionPolicy.validateRequested(
                    current,
                    CurationStatus.PUBLICADO))
                    .isInstanceOf(InvalidCurationTransitionException.class);
        }
    }
}
