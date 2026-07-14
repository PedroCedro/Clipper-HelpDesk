package br.com.infocedro.clipper.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RawSourceLabelsTest {

    @Test
    void traduzFonteConhecidaESegueNeutroParaNovaFonte() {
        assertThat(RawSourceLabels.labelFor("totvs-winthor")).isEqualTo("Fonte oficial");
        assertThat(RawSourceLabels.labelFor("nova-fonte")).isEqualTo("Fonte oficial");
    }
}
