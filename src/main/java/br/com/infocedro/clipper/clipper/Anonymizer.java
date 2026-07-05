package br.com.infocedro.clipper.clipper;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

// Anonimizador: mascara dado sensível em tokens ANTES de sair pra IA e des-mascara
// a resposta localmente. Não conhece Ticket nem LLM — só trabalha texto.
//
// O mapa token->valor real fica LOCAL (o chamador guarda). É ele que permite
// des-mascarar depois. Dado real nunca sai da rede.
//
// MVP: pega o que dá pra pegar bem com regex (CPF, CNPJ, valores em R$).
// Razão social / nomes de pessoas ficam pra depois (precisa NER ou dicionário).
@Component
public class Anonymizer {

    private static final Pattern CNPJ = Pattern.compile("\\b\\d{2}\\.\\d{3}\\.\\d{3}/\\d{4}-\\d{2}\\b");
    private static final Pattern CPF = Pattern.compile("\\b\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}\\b");
    private static final Pattern VALOR = Pattern.compile("R\\$\\s?\\d{1,3}(\\.\\d{3})*(,\\d{2})?");

    // Substitui dado sensível por tokens, acumulando no mapping (compartilhado
    // entre chamadas pra manter o mesmo token pro mesmo valor).
    public String mask(String text, Map<String, String> mapping) {
        if (text == null) {
            return null;
        }
        String out = text;
        // CNPJ antes de CPF: são formatos distintos, mas mantém a ordem explícita.
        out = CNPJ.matcher(out).replaceAll(m -> Matcher.quoteReplacement(tokenFor("CNPJ", m.group(), mapping)));
        out = CPF.matcher(out).replaceAll(m -> Matcher.quoteReplacement(tokenFor("CPF", m.group(), mapping)));
        out = VALOR.matcher(out).replaceAll(m -> Matcher.quoteReplacement(tokenFor("VALOR", m.group(), mapping)));
        return out;
    }

    // Troca os tokens de volta pelos valores reais.
    public String unmask(String text, Map<String, String> mapping) {
        if (text == null) {
            return null;
        }
        String out = text;
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            out = out.replace(entry.getKey(), entry.getValue());
        }
        return out;
    }

    // Reaproveita o token se o valor já foi mascarado; senão cria [TIPO_n].
    private String tokenFor(String type, String value, Map<String, String> mapping) {
        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        long n = mapping.keySet().stream().filter(k -> k.startsWith("[" + type + "_")).count() + 1;
        String token = "[" + type + "_" + n + "]";
        mapping.put(token, value);
        return token;
    }
}
