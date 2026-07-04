package br.com.infocedro.clipper.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Configuração do motor de IA (prefixo clipper.ai no application.properties).
// A chave é server-side e vem por variável de ambiente — nunca versionada.
@ConfigurationProperties(prefix = "clipper.ai")
public record AiProperties(String baseUrl, String apiKey, String model) {
}
