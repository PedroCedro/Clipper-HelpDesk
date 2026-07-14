package br.com.infocedro.clipper.catalog;

import java.time.OffsetDateTime;

/** Projeção leve: exclui HTML e metadataJson durante a pesquisa. */
public interface RawSearchCandidate {

    Long getId();
    String getExternalId();
    String getTitle();
    String getTextContent();
    String getSourceUrl();
    String getLabelsText();
    String getRoutinesText();
    String getErrorCodesText();
    String getModule();
    OffsetDateTime getSourceUpdatedAt();
}
