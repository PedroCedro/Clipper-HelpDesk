package br.com.infocedro.clipper.clipper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import br.com.infocedro.clipper.knowledge.KnowledgeArticle;
import br.com.infocedro.clipper.knowledge.KnowledgeSearch;

// Testes do orquestrador. Travam o CONTRATO do gate de grounding que o
// frontend vai consumir: prefixo "ancorado: " vs "sem-base: " no source.
// Se alguém mudar o prefixo, quebra aqui primeiro — não na tela.
//
// O Anonymizer entra de verdade (é determinístico e tem teste próprio);
// KnowledgeSearch e DiagnosticProvider são dublês, porque o que está em
// julgamento é só a coreografia do motor.
class DiagnosticEngineTest {

    @Test
    void comMatchNaBaseRespondeAncoradoSemChamarIa() {
        KnowledgeArticle artigo = new KnowledgeArticle(
                "Cupom fiscal não aparece na rotina 1443",
                "Sintoma... Causa provável... Próximos passos...");
        artigo.setSourceUrl("https://exemplo.com/artigo-1443");

        KnowledgeSearch knowledge = mock(KnowledgeSearch.class);
        when(knowledge.search(anyString())).thenReturn(Optional.of(artigo));

        // Provider-sentinela: se o motor encostar na IA com a base tendo
        // resposta, o teste falha — resposta curada não gasta token.
        DiagnosticProvider provider = request -> {
            throw new AssertionError("IA não deve ser chamada quando a base ancora");
        };

        DiagnosticEngine engine = new DiagnosticEngine(provider, new Anonymizer(), knowledge);
        DiagnosticResult result = engine.run(new DiagnosticRequest(
                "Cupom fiscal sumiu", "Não aparece na rotina 1443"));

        // O contrato do gate: prefixo + título + URL rastreável.
        assertTrue(result.source().startsWith("ancorado: "));
        assertTrue(result.source().contains("Cupom fiscal não aparece na rotina 1443"));
        assertTrue(result.source().contains("https://exemplo.com/artigo-1443"));
        // Curado oficial → confiança plena, sem autoavaliação de LLM.
        assertEquals(1.0, result.confidence());
        // O conteúdo curado chega inteiro pro técnico.
        assertEquals(artigo.getContent(), result.nextSteps());
    }

    @Test
    void semMatchChamaIaComTextoMascaradoEMarcaSemBase() {
        KnowledgeSearch knowledge = mock(KnowledgeSearch.class);
        when(knowledge.search(anyString())).thenReturn(Optional.empty());

        // Captura o que o motor mandou pra "IA" — é o ponto de auditoria da
        // anonimização: o dado real não pode cruzar esta fronteira.
        AtomicReference<DiagnosticRequest> recebidoPelaIa = new AtomicReference<>();
        DiagnosticProvider provider = request -> {
            recebidoPelaIa.set(request);
            // A IA responde citando o token mascarado, como faria de verdade.
            return new DiagnosticResult(
                    "Cadastro do CPF [CPF_1] inconsistente",
                    "Revisar o cadastro do cliente [CPF_1]",
                    0.7,
                    "gpt-4o-mini");
        };

        DiagnosticEngine engine = new DiagnosticEngine(provider, new Anonymizer(), knowledge);
        DiagnosticResult result = engine.run(new DiagnosticRequest(
                "Erro no faturamento",
                "Cliente CPF 123.456.789-01 não fatura"));

        // 1) O que saiu pra IA foi mascarado (token no lugar do CPF real).
        DiagnosticRequest masked = recebidoPelaIa.get();
        assertNotNull(masked, "provider deveria ter sido chamado no caminho sem base");
        assertFalse(masked.description().contains("123.456.789-01"));
        assertTrue(masked.description().contains("[CPF_1]"));

        // 2) A resposta volta des-mascarada pro técnico (token → dado real).
        assertTrue(result.probableCause().contains("123.456.789-01"));
        assertTrue(result.nextSteps().contains("123.456.789-01"));

        // 3) O gate marca a origem: sem lastro na base, e diz qual modelo foi.
        assertTrue(result.source().startsWith("sem-base: "));
        assertTrue(result.source().contains("gpt-4o-mini"));
    }
}
