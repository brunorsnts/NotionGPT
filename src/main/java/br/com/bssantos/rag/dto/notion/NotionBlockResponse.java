package br.com.bssantos.rag.dto.notion;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.Map;

@JsonIgnoreProperties({"object", "id", "parent", "created_time", "lasted_edited_time", "created_by", "last_edited_by", "in_trash"})
public class NotionBlockResponse {
        String type;
        @JsonProperty(value = "has_children") boolean hasChildren;
        Map<String, JsonNode> content = new HashMap<>();

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isHasChildren() {
        return hasChildren;
    }

    public void setHasChildren(boolean hasChildren) {
        this.hasChildren = hasChildren;
    }

    @JsonAnySetter
    public void adicionarConteudo(String chave, JsonNode valor) {
        this.content.put(chave, valor);
    }

    public JsonNode getContent() {
        return this.content.get(this.type);
    }
}
