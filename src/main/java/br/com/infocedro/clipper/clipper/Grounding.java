package br.com.infocedro.clipper.clipper;

// O gate de grounding em forma ESTRUTURADA — é isto que o frontend consome
// pra renderizar o selo, não o prefixo do source (string é pra humano/log;
// parsear string na UI quebraria em silêncio se o formato mudar).
//
// Quem preenche é o ORQUESTRADOR (DiagnosticEngine): o gate é decisão do
// motor; o provider não sabe em qual estado a resposta dele vai cair.
public record Grounding(State state, String articleTitle, String articleUrl, String model) {

    public enum State {
        // Artigo curado respondeu sozinho (match forte, sem IA).
        ANCORADO,
        // IA respondeu com artigo curado de apoio (match fraco, RAG-lite).
        APOIADO,
        // IA respondeu sozinha, sem lastro na base.
        SEM_BASE
    }

    // Fábricas nomeadas: deixam o engine legível e concentram aqui a regra
    // de quais campos fazem sentido em cada estado (ancorado não tem modelo;
    // sem-base não tem artigo).
    public static Grounding anchored(String articleTitle, String articleUrl) {
        return new Grounding(State.ANCORADO, articleTitle, articleUrl, null);
    }

    public static Grounding supported(String articleTitle, String articleUrl, String model) {
        return new Grounding(State.APOIADO, articleTitle, articleUrl, model);
    }

    public static Grounding ungrounded(String model) {
        return new Grounding(State.SEM_BASE, null, null, model);
    }
}
