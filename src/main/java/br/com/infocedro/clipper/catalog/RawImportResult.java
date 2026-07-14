package br.com.infocedro.clipper.catalog;

public record RawImportResult(String collectionId, int inserted, int updated, int unchanged) {
}
