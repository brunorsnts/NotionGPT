package br.com.bssantos.rag.service;

import br.com.bssantos.rag.dto.ChatRequest;
import br.com.bssantos.rag.dto.ChatResponse;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class QueryService {

    private final EmbeddingModel embeddingModel;
    private final ChatService chatService;
    private final EmbeddingStore<TextSegment> embeddingStore;

    public QueryService(@Qualifier("embeddingQuery") EmbeddingModel embeddingModel, ChatService chatService, EmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingModel = embeddingModel;
        this.chatService = chatService;
        this.embeddingStore = embeddingStore;
    }

    public ChatResponse askIA(ChatRequest request) {
        List<EmbeddingMatch<TextSegment>> result = result(searchRequest(embed(request)));
        return chatService.ask(request.query(), result);
    }

    private List<EmbeddingMatch<TextSegment>> result(EmbeddingSearchRequest searchRequest) {
        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
        return searchResult.matches();
    }

    private EmbeddingSearchRequest searchRequest(Embedding embedding) {
        return EmbeddingSearchRequest.builder()
                .queryEmbedding(embedding)
                .maxResults(4)
                .build();
    }

    private Embedding embed(ChatRequest request) {
        return embeddingModel.embed(request.query()).content();
    }
}
