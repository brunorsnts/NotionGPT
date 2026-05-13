package br.com.bssantos.rag.util;

import br.com.bssantos.rag.dto.notion.NotionBlockResponse;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class NotionBlockExtractor {

    private static final Set<String> tiposValidos = Set.of(
            "heading_1", "heading_2", "heading_3",
            "paragraph", "bulleted_list_item", "quote", "code"
    );

    public String extract(List<NotionBlockResponse> blocks) {

        return blocks.stream()
                .filter(b -> tiposValidos.contains(b.getType()))
                .map(b -> {
                    JsonNode richText = b.getContent().path("rich_text");
                    StringBuilder sb = new StringBuilder();
                    for (JsonNode node : richText) {
                        sb.append(node.path("plain_text").asText());
                    }
                    return sb.toString();
                })
                .filter(s -> !s.isBlank())
                .collect(Collectors.joining("\n"));
    }
}
