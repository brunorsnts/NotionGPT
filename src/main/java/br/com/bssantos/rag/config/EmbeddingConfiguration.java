package br.com.bssantos.rag.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.cohere.CohereEmbeddingModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pgvector.PgVectorEmbeddingStore;
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
    public EmbeddingStore<TextSegment> embeddingStore(@Value("${spring.datasource.username}") String username,
                                                      @Value("${spring.datasource.password}") String password) {

        return PgVectorEmbeddingStore.builder()
                .host("localhost")
                .port(5433)
                .user(username)
                .password(password)
                .database("notiongpt")
                .table("embeddings")
                .dimension(384)
                .build();
    }
}
