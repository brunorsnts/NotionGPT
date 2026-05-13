package br.com.bssantos.rag.dto.notion.properties;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NotionTitleProperty(
    @JsonProperty(value = "title") List<NotionRichText> notionRichTexts
) {
}
