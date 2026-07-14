package br.com.infocedro.clipper.collector;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Documento bruto vindo da TOTVS. Este tipo pertence ao adaptador da fonte;
 * o futuro núcleo de curadoria receberá um documento neutro, sem estes nomes
 * e metadados específicos do WinThor.
 */
public record TotvsArticleRecord(
        long id,
        String url,
        @JsonProperty("titulo") String title,
        @JsonProperty("secao_id") long sectionId,
        @JsonProperty("secao_nome") String sectionName,
        @JsonProperty("secao_caminho") String sectionPath,
        List<String> labels,
        @JsonProperty("conteudo_texto") String textContent,
        @JsonProperty("conteudo_html") String htmlContent,
        @JsonProperty("criado_em") String createdAt,
        @JsonProperty("atualizado_em") String updatedAt,
        @JsonProperty("source_api") String sourceApi,
        @JsonProperty("collected_at") String collectedAt
) {
}
