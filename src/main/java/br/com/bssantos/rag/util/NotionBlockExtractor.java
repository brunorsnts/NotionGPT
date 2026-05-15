package br.com.bssantos.rag.util;

import br.com.bssantos.rag.dto.notion.NotionBlockResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class NotionBlockExtractor {

    private static final Set<String> TIPOS_SUPORTADOS = Set.of(
            "heading_1", "heading_2", "heading_3",
            "paragraph", "bulleted_list_item", "quote", "code"
    );
    private static final Map<String, String> PREFIXOS_HEADING = Map.of(
            "heading_1", "#",
            "heading_2", "##",
            "heading_3", "###"
    );

    public String extract(List<NotionBlockResponse> blocks) {

        return blocks.stream()
                .filter(b -> TIPOS_SUPORTADOS.contains(b.getType()))
                .map(this::formatarBloco)
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining("\n")).strip();
    }

    private String extrairTexto(JsonNode richText) {
        StringBuilder sb = new StringBuilder();
        for (JsonNode node : richText) {
            sb.append(node.path("plain_text").asText());
        }
        return sb.toString();
    }

    private String formatarBloco(NotionBlockResponse notionBlockResponse) {
        JsonNode conteudo = notionBlockResponse.getContent();
        if (conteudo == null || conteudo.isMissingNode()) {
            return "";
        }
        JsonNode richText = conteudo.path("rich_text");
        String prefixo = PREFIXOS_HEADING.getOrDefault(notionBlockResponse.getType(), "");
        String texto = extrairTexto(richText);
        return prefixo.isEmpty() ? texto : "\n" + prefixo + " " + texto;

    }
}
