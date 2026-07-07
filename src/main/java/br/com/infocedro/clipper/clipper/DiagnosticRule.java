package br.com.infocedro.clipper.clipper;

// Esqueleto para regras determinísticas de diagnóstico.
// A ideia é cobrir casos conhecidos antes de recorrer ao LLM.
public class DiagnosticRule {

    private String name;
    private String description;

    public DiagnosticRule() {
    }

    public DiagnosticRule(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
