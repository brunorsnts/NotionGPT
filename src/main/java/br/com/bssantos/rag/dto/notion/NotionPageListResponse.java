package br.com.bssantos.rag.dto.notion;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NotionPageListResponse(
        List<NotionPageResponse> results,
        @JsonProperty(value = "has_more") boolean hasMore,
        @JsonProperty(value = "next_cursor") String nextCursor
) {
}
