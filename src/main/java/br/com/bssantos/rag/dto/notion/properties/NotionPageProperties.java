package br.com.bssantos.rag.dto.notion.properties;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NotionPageProperties(
        @JsonProperty(value = "Nome") NotionTitleProperty nome,
        @JsonProperty(value = "Trilha") NotionRelationProperty trilha,
        @JsonProperty(value = "Status") NotionStatusProperty status
) {
}
