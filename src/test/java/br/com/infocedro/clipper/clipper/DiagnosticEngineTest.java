package br.com.infocedro.clipper.clipper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import br.com.infocedro.clipper.knowledge.KnowledgeArticle;
import br.com.infocedro.clipper.knowledge.KnowledgeMatch;
import br.com.infocedro.clipper.knowledge.KnowledgeSearch;

// Testes do orquestrador. Travam o CONTRATO do gate de grounding que o
// frontend vai consumir — os três estados no prefixo do source:
// "ancorado: " (curado, sem IA), "apoiado: " (IA com material curado) e
// "sem-base: " (IA sozinha). Se alguém mudar um prefixo, quebra aqui
// primeiro — não na tela.
//
// O Anonymizer entra de verdade (é determinístico e tem teste próprio);
// KnowledgeSearch e DiagnosticProvider são dublês, porque o que está em
// julgamento é só a coreografia do motor.
class DiagnosticEngineTest {

    @Test
    void matchForteRespondeAncoradoSemChamarIa() {
        KnowledgeSearch knowledge = mock(KnowledgeSearch.class);
        when(knowledge.search(anyString()))
                .thenReturn(Optional.of(match(artigo1443(), KnowledgeMatch.Strength.STRONG)));

        // Provider-sentinela: se o motor encostar na IA com a base tendo
        // resposta forte, o teste falha — resposta curada não gasta token.
        DiagnosticProvider provider = (request, material) -> {
            throw new AssertionError("IA não deve ser chamada quando o match é forte");
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
        assertEquals(artigo1443().getContent(), result.nextSteps());

        // Grounding estruturado — o que o frontend consome pro selo (sem
        // parsear string). Ancorado não tem modelo: IA não participou.
        assertEquals(Grounding.State.ANCORADO, result.grounding().state());
        assertEquals("Cupom fiscal não aparece na rotina 1443", result.grounding().articleTitle());
        assertEquals("https://exemplo.com/artigo-1443", result.grounding().articleUrl());
        assertNull(result.grounding().model());
    }

    @Test
    void matchFracoChamaIaComArtigoDeApoioEMarcaApoiado() {
        KnowledgeArticle artigo = artigo1443();
        KnowledgeSearch knowledge = mock(KnowledgeSearch.class);
        when(knowledge.search(anyString()))
                .thenReturn(Optional.of(match(artigo, KnowledgeMatch.Strength.WEAK)));

        // Captura o que o motor entregou pra "IA": o chamado mascarado E o
        // material de apoio (RAG-lite).
        AtomicReference<DiagnosticRequest> pedidoRecebido = new AtomicReference<>();
        AtomicReference<KnowledgeContext> materialRecebido = new AtomicReference<>();
        DiagnosticProvider provider = (request, material) -> {
            pedidoRecebido.set(request);
            materialRecebido.set(material);
            return new DiagnosticResult(
                    "Vendas presas no caixa do CPF [CPF_1]",
                    "Seguir o artigo e reprocessar na 2097",
                    0.8,
                    "gpt-4o-mini");
        };

        DiagnosticEngine engine = new DiagnosticEngine(provider, new Anonymizer(), knowledge);
        DiagnosticResult result = engine.run(new DiagnosticRequest(
                "Cupom sumido",
                "Operador CPF 123.456.789-01 relata cupom que não sobe"));

        // 1) O artigo cruzou a costura como CONTRATO (não como entidade),
        //    com o conteúdo curado íntegro pro prompt.
        KnowledgeContext material = materialRecebido.get();
        assertNotNull(material, "match fraco deveria levar material de apoio pra IA");
        assertEquals(artigo.getTitle(), material.title());
        assertEquals(artigo.getContent(), material.content());

        // 2) Mesmo com apoio, o chamado segue mascarado (anonimização não
        //    depende do caminho).
        assertFalse(pedidoRecebido.get().description().contains("123.456.789-01"));
        assertTrue(pedidoRecebido.get().description().contains("[CPF_1]"));

        // 3) A resposta volta des-mascarada e carimbada como apoiada:
        //    artigo rastreável + qual modelo costurou.
        assertTrue(result.probableCause().contains("123.456.789-01"));
        assertTrue(result.source().startsWith("apoiado: "));
        assertTrue(result.source().contains("Cupom fiscal não aparece na rotina 1443"));
        assertTrue(result.source().contains("via gpt-4o-mini"));
        // 0.8 está abaixo do teto do estado apoiado (0.9) → passa intacto.
        // O teto é teto, não piso — a autoavaliação baixa do LLM é respeitada.
        assertEquals(0.8, result.confidence());

        // 4) Grounding estruturado: apoiado tem artigo E modelo (foi a IA que
        //    costurou, com o artigo de lastro).
        assertEquals(Grounding.State.APOIADO, result.grounding().state());
        assertEquals(artigo.getTitle(), result.grounding().articleTitle());
        assertEquals("gpt-4o-mini", result.grounding().model());
    }

    @Test
    void tetoDeConfiancaCortaAutoavaliacaoInflada() {
        // A "mentira viva" do Degrau 3: sem lastro pleno, o LLM ainda pode se
        // dar 0.95. O gate corta no teto do estado — apoiado nunca passa de
        // 0.9 (só curado verbatim vale 1.0). É o que impede um selo de
        // confiança inflado de chegar na tela do técnico.
        KnowledgeSearch knowledge = mock(KnowledgeSearch.class);
        when(knowledge.search(anyString()))
                .thenReturn(Optional.of(match(artigo1443(), KnowledgeMatch.Strength.WEAK)));

        DiagnosticProvider provider = (request, material) -> new DiagnosticResult(
                "causa", "passos", 0.95, "gpt-4o-mini");

        DiagnosticEngine engine = new DiagnosticEngine(provider, new Anonymizer(), knowledge);
        DiagnosticResult result = engine.run(new DiagnosticRequest("Cupom sumido", "detalhe"));

        assertEquals(0.9, result.confidence());
    }

    @Test
    void semMatchChamaIaComTextoMascaradoEMarcaSemBase() {
        KnowledgeSearch knowledge = mock(KnowledgeSearch.class);
        when(knowledge.search(anyString())).thenReturn(Optional.empty());

        // Captura o que o motor mandou pra "IA" — é o ponto de auditoria da
        // anonimização: o dado real não pode cruzar esta fronteira.
        AtomicReference<DiagnosticRequest> pedidoRecebido = new AtomicReference<>();
        AtomicReference<KnowledgeContext> materialRecebido = new AtomicReference<>();
        DiagnosticProvider provider = (request, material) -> {
            pedidoRecebido.set(request);
            materialRecebido.set(material);
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

        // 1) O que saiu pra IA foi mascarado (token no lugar do CPF real) e
        //    SEM material de apoio — sem match não há o que apoiar.
        DiagnosticRequest masked = pedidoRecebido.get();
        assertNotNull(masked, "provider deveria ter sido chamado no caminho sem base");
        assertNull(materialRecebido.get());
        assertFalse(masked.description().contains("123.456.789-01"));
        assertTrue(masked.description().contains("[CPF_1]"));

        // 2) A resposta volta des-mascarada pro técnico (token → dado real).
        assertTrue(result.probableCause().contains("123.456.789-01"));
        assertTrue(result.nextSteps().contains("123.456.789-01"));

        // 3) O gate marca a origem: sem lastro na base, e diz qual modelo foi.
        assertTrue(result.source().startsWith("sem-base: "));
        assertTrue(result.source().contains("gpt-4o-mini"));

        // 4) Sem base, a confiança nunca passa de "talvez": o 0.7 que o LLM
        //    se deu é cortado no teto do estado sem-base (0.5).
        assertEquals(0.5, result.confidence());

        // 5) Grounding estruturado: sem-base não tem artigo, só o modelo.
        assertEquals(Grounding.State.SEM_BASE, result.grounding().state());
        assertNull(result.grounding().articleTitle());
        assertEquals("gpt-4o-mini", result.grounding().model());
    }

    // Artigo de exemplo compartilhado pelos cenários (recriado a cada uso pra
    // um teste não enxergar mutação feita por outro).
    private KnowledgeArticle artigo1443() {
        KnowledgeArticle artigo = new KnowledgeArticle(
                "Cupom fiscal não aparece na rotina 1443",
                "Sintoma... Causa provável... Próximos passos...");
        artigo.setSourceUrl("https://exemplo.com/artigo-1443");
        return artigo;
    }

    private KnowledgeMatch match(KnowledgeArticle artigo, KnowledgeMatch.Strength strength) {
        return new KnowledgeMatch(artigo, strength);
    }
}
