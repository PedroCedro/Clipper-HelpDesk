package br.com.infocedro.clipper.catalog;

/** Critérios internos da busca; a borda HTTP será adicionada no C1.3. */
public record RawSearchQuery(
        String text,
        String sourceType,
        String module,
        int page,
        int size
) {
}
