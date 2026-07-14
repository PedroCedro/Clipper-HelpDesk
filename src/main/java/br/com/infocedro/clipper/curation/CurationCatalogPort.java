package br.com.infocedro.clipper.curation;

/** Fronteira mínima usada pela curadoria para validar documentos candidatos. */
public interface CurationCatalogPort {

    boolean exists(Long documentId);

    CurationCatalogDocument find(Long documentId);
}
