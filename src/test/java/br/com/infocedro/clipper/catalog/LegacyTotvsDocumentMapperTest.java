package br.com.infocedro.clipper.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class LegacyTotvsDocumentMapperTest {

    private final LegacyTotvsDocumentMapper mapper = new LegacyTotvsDocumentMapper(
            new ObjectMapper().findAndRegisterModules(), new DomainIdentifiersExtractor());

    @Test
    void traduzModuloLabelsEIdentificadoresDoRegistroLegado() {
        var row = new LegacyTotvsDocumentMapper.LegacyRow(
                42L, "https://fonte/42", "Erro na rotina 1443", 3L, "3 - Vendas",
                "3 - Vendas", "[\"Servidor\",\"Caixas\"]",
                "A rotina 1443 apresenta erro 1026", "<p>Texto</p>",
                "2026-01-01T10:00:00Z", "data-inválida", "https://api", "2026-07-14T10:00:00-03:00");

        RawDocumentCandidate candidate = mapper.map(row);

        assertThat(candidate.sourceType()).isEqualTo("totvs-winthor");
        assertThat(candidate.module()).isEqualTo("03-vendas");
        assertThat(candidate.labelsText()).isEqualTo("|servidor|caixas|");
        assertThat(candidate.routinesText()).isEqualTo("|1443|");
        assertThat(candidate.errorCodesText()).isEqualTo("|1026|");
        assertThat(candidate.sourceUpdatedAt()).isNull();
    }

    @Test
    void usaRaizDaTrilhaSemConfundirNomeDaFolha() {
        RawDocumentCandidate receiving = mapper.map(row(
                "Módulo 18", "13 - Recebimento de Mercadoria > Módulo 18"));
        RawDocumentCandidate rejection = mapper.map(row(
                "Rejeições", "14 - Faturamento > Rejeições"));
        RawDocumentCandidate taxReform = mapper.map(row(
                "RT07 – Fiscal", "Reforma Tributária - Winthor > RT07 – Fiscal"));
        RawDocumentCandidate supermarket = mapper.map(row(
                "PDV Supermercados", "PDV Supermercados"));

        assertThat(receiving.module()).isEqualTo("13-recebimento-mercadoria");
        assertThat(rejection.module()).isEqualTo("14-faturamento");
        assertThat(taxReform.module()).isEqualTo("reforma-tributaria-winthor");
        assertThat(supermarket.module()).isEqualTo("pdv-supermercados");
    }

    @Test
    void usaChaveOficialDaAdministracaoInterna() {
        assertThat(mapper.map(row(
                "11 - Adm. Interna do Estoque", "11 - Adm. Interna do Estoque")).module())
                .isEqualTo("11-adm-interna-estoque");
    }

    private LegacyTotvsDocumentMapper.LegacyRow row(String sectionName, String sectionPath) {
        return new LegacyTotvsDocumentMapper.LegacyRow(
                1L, "https://fonte/1", "Título", 1L, sectionName, sectionPath,
                "[]", "Texto", "<p>Texto</p>", null, null, null, null);
    }
}
