package br.com.bssantos.rag.dto.notion.properties;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NotionStatusProperty(
        NotionStatusValue status
) {
}
