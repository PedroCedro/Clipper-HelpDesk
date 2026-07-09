package br.com.infocedro.clipper.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

// Testes do retrieval. É AQUI que o limiar MIN_TOKEN_HITS deixa de ser um
// número chutado e vira comportamento travado: se alguém mudar o valor ou a
// regra de match, um destes testes quebra e conta o porquê.
//
// Teste de unidade puro: nada de Spring/H2 subindo. O repositório é um mock
// que EMULA a semântica da derived query (substring, ignore case) sobre uma
// lista fixa — assim o teste exercita a mesma mecânica da produção.
class KnowledgeSearchTest {

    private KnowledgeRepository repository;
    private KnowledgeSearch search;

    // Mini-seed do teste: espelha o formato real (keywords em CSV), mas é
    // independente do artigos.yaml — o teste não pode quebrar porque a
    // curadoria mudou.
    private KnowledgeArticle artigo1443;
    private KnowledgeArticle artigo2097;

    @BeforeEach
    void setUp() {
        artigo1443 = artigo(1L, "Cupom fiscal não aparece na rotina 1443",
                "1443, cupom fiscal, vendas presas, nfc-e, 2097");
        artigo2097 = artigo(2L, "Como processar cupom fiscal em contingência",
                "2097, contingência, contingencia, cupom fiscal, nfc-e, a enviar, dpec");

        List<KnowledgeArticle> base = List.of(artigo1443, artigo2097);

        repository = mock(KnowledgeRepository.class);
        // Emula findByKeywordsContainingIgnoreCase: substring, sem caixa.
        // É de propósito o comportamento "largo" do banco — quem refina é o
        // KnowledgeSearch, e é isso que os testes abaixo cobram.
        when(repository.findByKeywordsContainingIgnoreCase(anyString())).thenAnswer(inv -> {
            String term = inv.getArgument(0, String.class).toLowerCase();
            return base.stream()
                    .filter(a -> a.getKeywords().toLowerCase().contains(term))
                    .toList();
        });

        search = new KnowledgeSearch(repository);
    }

    @Test
    void ancoraQuandoDoisOuMaisTokensCasamNoMesmoArtigo() {
        // "cupom" + "fiscal" + "1443" batem no mesmo artigo → match forte.
        Optional<KnowledgeArticle> hit = search.search("Cupom fiscal sumiu na rotina 1443");

        assertTrue(hit.isPresent());
        assertEquals("Cupom fiscal não aparece na rotina 1443", hit.get().getTitle());
    }

    @Test
    void naoAncoraComUmTokenSo() {
        // Só "cupom" casa — 1 token é ruído, não âncora. É o teste que
        // justifica o MIN_TOKEN_HITS >= 2: sem ele, qualquer palavra comum
        // falsearia o selo "ancorado" do gate de grounding.
        Optional<KnowledgeArticle> hit = search.search("problema estranho no cupom hoje");

        assertTrue(hit.isEmpty());
    }

    @Test
    void naoAncoraComQueryVaziaOuNula() {
        assertTrue(search.search(null).isEmpty());
        assertTrue(search.search("   ").isEmpty());
    }

    @Test
    void venceOArtigoComMaisTokensCasados() {
        // "cupom fiscal" casa nos DOIS artigos; "contingência" e "dpec" só no
        // 2097 → o placar decide o vencedor, não a ordem da lista.
        Optional<KnowledgeArticle> hit = search.search("cupom fiscal em contingência dpec");

        assertTrue(hit.isPresent());
        assertEquals("Como processar cupom fiscal em contingência", hit.get().getTitle());
    }

    @Test
    void tokenNaoCasaPorPedacoDeKeyword() {
        // "144" é substring de "1443" no banco, mas rotina 144 ≠ rotina 1443.
        // Ancorar aqui entregaria ao técnico a solução do problema ERRADO —
        // pior que responder "sem base". O token só vale se for palavra
        // INTEIRA das keywords.
        Optional<KnowledgeArticle> hit = search.search("cupom com erro na rotina 144");

        assertTrue(hit.isEmpty());
    }

    // A entidade não expõe setId (id é do banco). No teste, injetamos via
    // reflexão só pra distinguir os artigos no placar do KnowledgeSearch.
    private KnowledgeArticle artigo(Long id, String title, String keywords) {
        KnowledgeArticle artigo = new KnowledgeArticle(title, "conteúdo de " + title);
        artigo.setKeywords(keywords);
        ReflectionTestUtils.setField(artigo, "id", id);
        return artigo;
    }
}
