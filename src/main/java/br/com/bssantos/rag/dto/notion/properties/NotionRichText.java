package br.com.bssantos.rag.dto.notion.properties;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NotionRichText(
        @JsonProperty(value = "plain_text") String plainText
) {
}
