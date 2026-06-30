package br.com.infocedro.clipper.clipper;

// Contrato de entrada do módulo "clipper": é tudo que o motor de diagnóstico
// precisa saber pra trabalhar. Repare que NÃO mencionamos a entidade Ticket —
// é exatamente isso que desacopla o clipper do módulo ticket (a costura).

public record DiagnosticRequest(String title, String description) {
}
