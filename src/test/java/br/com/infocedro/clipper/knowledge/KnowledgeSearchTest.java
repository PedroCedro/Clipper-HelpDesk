package br.com.infocedro.clipper.knowledge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
// número chutado e vira comportamento travado: 2+ tokens = match FORTE
// (lookup puro), 1 token = match FRACO (vai de apoio pra IA), 0 = nada.
// Se alguém mudar o valor ou a regra de match, um destes testes quebra e
// conta o porquê.
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
    private KnowledgeArticle artigo1360;

    @BeforeEach
    void setUp() {
        artigo1443 = artigo(1L, "Cupom fiscal não aparece na rotina 1443",
                "1443, cupom fiscal, vendas presas, nfc-e, 2097");
        artigo2097 = artigo(2L, "Como processar cupom fiscal em contingência",
                "2097, contingência, contingencia, cupom fiscal, nfc-e, a enviar, dpec");
        artigo1360 = artigo(3L, "Devolução de venda em contingência",
                "1360, devolução, nf-e não consta, nf-e nao consta, mercadoria, sefaz");

        List<KnowledgeArticle> base = List.of(artigo1443, artigo2097, artigo1360);

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
    void doisOuMaisTokensNoMesmoArtigoEhMatchForte() {
        // "cupom" + "fiscal" + "1443" batem no mesmo artigo → forte: o motor
        // pode responder com o texto curado direto, sem IA.
        Optional<KnowledgeMatch> hit = search.search("Cupom fiscal sumiu na rotina 1443");

        assertTrue(hit.isPresent());
        assertTrue(hit.get().isStrong());
        assertEquals("Cupom fiscal não aparece na rotina 1443", hit.get().article().getTitle());
    }

    @Test
    void umTokenSoEhMatchFracoNaoAncora() {
        // Só "cupom" casa — 1 token não sustenta o selo "ancorado" (qualquer
        // palavra comum falsearia o gate), mas é pista boa demais pra jogar
        // fora: vira match FRACO, que o motor manda de apoio pra IA.
        Optional<KnowledgeMatch> hit = search.search("problema estranho no cupom hoje");

        assertTrue(hit.isPresent());
        assertFalse(hit.get().isStrong());
    }

    @Test
    void semTokenCasandoNaoHaMatch() {
        // Nenhuma palavra do ticket existe nas keywords → vazio de verdade
        // (é o caminho que termina em "sem-base" no motor).
        assertTrue(search.search("impressora travada no balcão").isEmpty());
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
        Optional<KnowledgeMatch> hit = search.search("cupom fiscal em contingência dpec");

        assertTrue(hit.isPresent());
        assertTrue(hit.get().isStrong());
        assertEquals("Como processar cupom fiscal em contingência", hit.get().article().getTitle());
    }

    @Test
    void tokenNaoCasaPorPedacoDeKeyword() {
        // "144" é substring de "1443" no banco, mas rotina 144 ≠ rotina 1443.
        // Contar esse ponto entregaria ao técnico a solução do problema ERRADO.
        // O token só vale se for palavra INTEIRA das keywords — aqui nenhuma
        // palavra do ticket é keyword, então nem match fraco pode haver.
        Optional<KnowledgeMatch> hit = search.search("erro estranho na rotina 144");

        assertTrue(hit.isEmpty());
    }

    @Test
    void stopwordNaoSozinhaNaoPontua() {
        // "nao" existe nas keywords do artigo 1360, mas é ruído linguístico:
        // sem outro termo de domínio, nem match fraco deve ser produzido.
        Optional<KnowledgeMatch> hit = search.search("impressora nao liga");

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
