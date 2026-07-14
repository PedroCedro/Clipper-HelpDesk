package br.com.infocedro.clipper.catalog;

/** Contexto confiável vindo do manifesto validado da coleta. */
public record RawImportContext(String sourceType, String scope, int schemaVersion) {
}
