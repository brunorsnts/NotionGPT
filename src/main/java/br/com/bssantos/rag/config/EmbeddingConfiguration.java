package br.com.bssantos.rag.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.cohere.CohereEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingConfiguration {

    @Value("${cohere-api-key}")
    private String apiKey;

    @Bean
    public EmbeddingModel embeddingDocument() {
        return CohereEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName("embed-multilingual-light-v3.0")
                .inputType("search_document")
                .build();
    }

    @Bean
    public EmbeddingModel embeddingQuery() {
        return CohereEmbeddingModel.builder()
                .apiKey(apiKey)
                .modelName("embed-multilingual-light-v3.0")
                .inputType("search_query")
                .build();
    }

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        return new InMemoryEmbeddingStore<>();
    }
}
