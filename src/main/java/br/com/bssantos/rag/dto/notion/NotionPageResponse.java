package br.com.bssantos.rag.dto.notion;

import br.com.bssantos.rag.dto.notion.properties.NotionPageProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NotionPageResponse(
        String id,
        @JsonProperty(value = "last_edited_time") String lastEditedTime,
        NotionPageProperties properties
) {

    public UUID idAsUUID() {
        return UUID.fromString(this.id);
    }

    public Instant lastEditedTimeAsInstant() {
        return Instant.parse(this.lastEditedTime);
    }

    public String titulo() {
        return properties.nome().notionRichTexts().get(0).plainText();
    }
}
