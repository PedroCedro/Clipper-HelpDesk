package br.com.infocedro.clipper.clipper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class AnonymizerTest {

    private final Anonymizer anonymizer = new Anonymizer();

    @Test
    void mascaraCpfCnpjEValor() {
        Map<String, String> mapping = new HashMap<>();
        String texto = "Cliente 123.456.789-01 da empresa 12.345.678/0001-90 com debito de R$ 1.234,56";
        String masked = anonymizer.mask(texto, mapping);

        assertTrue(masked.contains("[CPF_1]"));
        assertTrue(masked.contains("[CNPJ_1]"));
        assertTrue(masked.contains("[VALOR_1]"));
        // dado real não pode vazar no texto mascarado
        assertFalse(masked.contains("123.456.789-01"));
        assertFalse(masked.contains("12.345.678/0001-90"));
        assertFalse(masked.contains("R$ 1.234,56"));
    }

    @Test
    void desmascaraRestauraOriginal() {
        Map<String, String> mapping = new HashMap<>();
        String texto = "CPF 123.456.789-01 e CNPJ 12.345.678/0001-90";
        String masked = anonymizer.mask(texto, mapping);
        String restaurado = anonymizer.unmask(masked, mapping);

        assertEquals(texto, restaurado);
    }

    @Test
    void mesmoValorReusaMesmoToken() {
        Map<String, String> mapping = new HashMap<>();
        String masked = anonymizer.mask("A 123.456.789-01 e B 123.456.789-01", mapping);

        assertEquals("A [CPF_1] e B [CPF_1]", masked);
        assertEquals(1, mapping.size());
    }

    @Test
    void mapaCompartilhadoEntreCampos() {
        Map<String, String> mapping = new HashMap<>();
        String titulo = anonymizer.mask("CNPJ 12.345.678/0001-90", mapping);
        String descricao = anonymizer.mask("mesmo CNPJ 12.345.678/0001-90 de novo", mapping);

        assertTrue(titulo.contains("[CNPJ_1]"));
        assertTrue(descricao.contains("[CNPJ_1]"));
        assertEquals(1, mapping.size());
    }

    @Test
    void textoSemDadoSensivelNaoMuda() {
        Map<String, String> mapping = new HashMap<>();
        String texto = "Erro na rotina 1402 ao emitir NF-e";

        assertEquals(texto, anonymizer.mask(texto, mapping));
        assertTrue(mapping.isEmpty());
    }
}
