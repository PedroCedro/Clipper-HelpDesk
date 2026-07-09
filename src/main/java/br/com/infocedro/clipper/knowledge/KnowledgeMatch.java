package br.com.infocedro.clipper.knowledge;

// Resultado do retrieval COM a força do match — é ela que decide o destino
// no motor: forte → lookup puro (artigo verbatim, sem IA); fraco → RAG
// (artigo vira material de apoio no prompt da IA).
//
// Nasceu no Degrau 2: devolver só o artigo (Optional<KnowledgeArticle>)
// escondia a informação "quão bom foi o match", e o motor precisa dela.
public record KnowledgeMatch(KnowledgeArticle article, Strength strength) {

    public enum Strength {
        // 2+ tokens do ticket casaram no artigo: confiança pra responder
        // com o texto curado direto, sem gastar IA.
        STRONG,
        // 1 token só: sugestivo demais pra ignorar, fraco demais pra
        // ancorar — a IA costura o artigo ao caso do ticket.
        WEAK
    }

    public boolean isStrong() {
        return strength == Strength.STRONG;
    }
}
