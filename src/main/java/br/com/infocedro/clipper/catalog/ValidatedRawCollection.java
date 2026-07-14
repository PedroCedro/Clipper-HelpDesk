package br.com.infocedro.clipper.catalog;

import java.nio.file.Path;

/** Coleta cuja estrutura e hashes já foram verificados. */
public record ValidatedRawCollection(
        String collectionId,
        int schemaVersion,
        String sourceType,
        String scope,
        Path documentsFile
) {
}
