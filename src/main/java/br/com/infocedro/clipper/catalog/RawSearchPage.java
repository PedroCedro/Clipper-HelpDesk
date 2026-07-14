package br.com.infocedro.clipper.catalog;

import java.util.List;

/** Página calculada depois do ranking global dos candidatos. */
public record RawSearchPage(List<RawSearchItem> items, int page, int size, long total) {
}
