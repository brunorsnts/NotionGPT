package br.com.bssantos.rag.dto.notion;

import br.com.bssantos.rag.dto.notion.properties.NotionPageProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NotionPageResponse(
        String id,
        @JsonProperty(value = "last_edited_time") String lastEditedTime,
        NotionPageProperties properties
) {
}
