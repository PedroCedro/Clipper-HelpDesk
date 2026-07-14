package br.com.infocedro.clipper.collector;

/**
 * Porta de entrada para qualquer fonte coletável. O núcleo conhece apenas o
 * identificador e o resultado comum; detalhes de API, autenticação e
 * navegação pertencem à implementação da fonte.
 */
public interface KnowledgeSource {

    String type();

    CollectionSummary collect() throws Exception;
}
