package br.com.infocedro.clipper.clipper;

// Falha ao consultar o provider de IA (chave ausente, rede, 401/429, resposta inválida).
// Unchecked de propósito: o motor não sabe se recupera; por ora a falha sobe honesta.
public class DiagnosticProviderException extends RuntimeException {

    public DiagnosticProviderException(String message) {
        super(message);
    }

    public DiagnosticProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
