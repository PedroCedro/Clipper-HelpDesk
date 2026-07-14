package br.com.infocedro.clipper.catalog;

/** Resumo idempotente da ponte do acervo legado para o catálogo atual. */
public record LegacyCatalogMigrationResult(int inserted, int updated, int unchanged) {
}
