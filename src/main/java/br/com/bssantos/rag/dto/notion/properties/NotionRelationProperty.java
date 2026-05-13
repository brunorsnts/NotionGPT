package br.com.bssantos.rag.dto.notion.properties;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NotionRelationProperty(
        List<NotionRelationItem> relation
) {
}
