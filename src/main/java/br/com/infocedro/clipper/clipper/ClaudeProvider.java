package br.com.infocedro.clipper.clipper;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import br.com.infocedro.clipper.config.AiProperties;

// Provider de IA usando o SDK Java oficial da Anthropic (com.anthropic:anthropic-java).
// Claude NÃO é OpenAI-compatível — por isso tem classe própria (ao contrário de
// Groq/DeepSeek/Ollama, que reusam o OpenAiCompatibleProvider).
//
// Só é ativado quando clipper.ai.provider=claude (senão o bean nem é criado, e
// não exige chave da Anthropic no startup).
@Component
@ConditionalOnProperty(name = "clipper.ai.provider", havingValue = "claude")
public class ClaudeProvider implements DiagnosticProvider {

    private static final String SYSTEM_PROMPT = """
            Você é o Clipper, um assistente técnico de helpdesk do ERP WinThor.
            Analise o chamado e responda SEMPRE em JSON com exatamente estas chaves:
              "causa_provavel": string — a causa mais provável, objetiva;
              "proximos_passos": string — o que o técnico deve fazer, em passos claros;
              "confianca": número de 0 a 1 — sua confiança no diagnóstico.
            Responda em português. Não invente dados que não estão no chamado.
            """;

    private final AnthropicClient client;
    private final AiProperties properties;
    private final ObjectMapper objectMapper;

    public ClaudeProvider(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.client = AnthropicOkHttpClient.builder()
                .apiKey(properties.apiKey())
                .build();
    }

    @Override
    public DiagnosticResult diagnose(DiagnosticRequest request) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new DiagnosticProviderException(
                    "Chave de IA ausente. Defina a variável de ambiente com a chave da Anthropic.");
        }

        try {
            MessageCreateParams params = MessageCreateParams.builder()
                    .model(properties.model())
                    .maxTokens(1024L)
                    .system(SYSTEM_PROMPT)
                    .addUserMessage(userContent(request))
                    .build();

            Message message = client.messages().create(params);

            StringBuilder text = new StringBuilder();
            message.content().stream()
                    .flatMap(block -> block.text().stream())
                    .forEach(block -> text.append(block.text()));

            return parse(text.toString());
        } catch (DiagnosticProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new DiagnosticProviderException(
                    "Falha ao chamar o Claude (" + properties.model() + "): " + e.getMessage(), e);
        }
    }

    private String userContent(DiagnosticRequest request) {
        return "Título do chamado: " + request.title() + "\n"
                + "Descrição: " + request.description();
    }

    // Diferente do provider OpenAI-compatível, aqui o SDK já entrega o texto
    // final do assistente; por isso só precisamos parsear o JSON interno.
    private DiagnosticResult parse(String content) {
        try {
            JsonNode diagnosis = objectMapper.readTree(content);
            return new DiagnosticResult(
                    diagnosis.path("causa_provavel").asText(""),
                    diagnosis.path("proximos_passos").asText(""),
                    diagnosis.path("confianca").asDouble(0.0),
                    properties.model());
        } catch (Exception e) {
            throw new DiagnosticProviderException(
                    "Resposta do Claude em formato inesperado: " + e.getMessage(), e);
        }
    }
}
