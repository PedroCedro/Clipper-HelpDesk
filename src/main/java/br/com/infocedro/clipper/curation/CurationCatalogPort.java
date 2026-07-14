package br.com.infocedro.clipper.curation;

import java.util.Collection;
import java.util.List;

/** Fronteira mínima usada pela curadoria para validar documentos candidatos. */
public interface CurationCatalogPort {

    boolean exists(Long documentId);

    List<CurationCatalogDocument> findSummaries(Collection<Long> documentIds);
}
