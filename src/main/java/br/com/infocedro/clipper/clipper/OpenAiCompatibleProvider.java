package br.com.infocedro.clipper.clipper;

import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.infocedro.clipper.config.AiProperties;

// Provider de IA falando a API OpenAI-compatível (/chat/completions).
// Mesma API serve OpenAI, Groq, DeepSeek, OpenRouter, Ollama — troca base_url+key+model.
// MVP: GPT-4o-mini. Usa o RestClient do Spring (sem SDK externo).
@Component
@ConditionalOnProperty(name = "clipper.ai.provider", havingValue = "openai", matchIfMissing = true)
public class OpenAiCompatibleProvider implements DiagnosticProvider {

    private static final String SYSTEM_PROMPT = """
            Você é o Clipper, um assistente técnico de helpdesk do ERP WinThor.
            Analise o chamado e responda SEMPRE em JSON com exatamente estas chaves:
              "causa_provavel": string — a causa mais provável, objetiva;
              "proximos_passos": string — o que o técnico deve fazer, em passos claros;
              "confianca": número de 0 a 1 — sua confiança no diagnóstico.
            Responda em português. Não invente dados que não estão no chamado.
            """;

    private final RestClient restClient;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleProvider(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
    }

    @Override
    public DiagnosticResult diagnose(DiagnosticRequest request) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new DiagnosticProviderException(
                    "Chave de IA ausente. Defina a variável de ambiente OPENAI_API_KEY.");
        }

        Map<String, Object> body = Map.of(
                "model", properties.model(),
                "messages", List.of(
                        Map.of("role", "system", "content", SYSTEM_PROMPT),
                        Map.of("role", "user", "content", userContent(request))),
                "response_format", Map.of("type", "json_object"));

        String rawResponse;
        try {
            rawResponse = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            throw new DiagnosticProviderException(
                    "Falha ao chamar o provider de IA (" + properties.model() + "): " + e.getMessage(), e);
        }

        return parse(rawResponse);
    }

    private String userContent(DiagnosticRequest request) {
        return "Título do chamado: " + request.title() + "\n"
                + "Descrição: " + request.description();
    }

    // A resposta traz choices[0].message.content, que por sua vez é uma STRING JSON
    // (por causa do response_format json_object). Então são dois níveis de parse.
    private DiagnosticResult parse(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            String content = root.path("choices").path(0).path("message").path("content").asText();
            JsonNode diagnosis = objectMapper.readTree(content);

            return new DiagnosticResult(
                    diagnosis.path("causa_provavel").asText(""),
                    diagnosis.path("proximos_passos").asText(""),
                    diagnosis.path("confianca").asDouble(0.0),
                    properties.model());
        } catch (Exception e) {
            throw new DiagnosticProviderException(
                    "Resposta do provider de IA em formato inesperado: " + e.getMessage(), e);
        }
    }
}
