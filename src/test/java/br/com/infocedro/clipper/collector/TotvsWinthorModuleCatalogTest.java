package br.com.infocedro.clipper.collector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class TotvsWinthorModuleCatalogTest {

    @Test
    void carregaCatalogoVersionadoEEncontraModulo() throws Exception {
        TotvsWinthorModuleCatalog catalog = new TotvsWinthorModuleCatalog();

        TotvsWinthorModule module = catalog.require("14-faturamento");

        assertEquals(360003676931L, module.rootSectionId());
        assertEquals("14 - Faturamento", module.name());
        assertEquals(8, catalog.all().size());
    }

    @Test
    void rejeitaModuloDesconhecido() throws Exception {
        TotvsWinthorModuleCatalog catalog = new TotvsWinthorModuleCatalog();

        assertThrows(IllegalArgumentException.class, () -> catalog.require("inexistente"));
    }
}
