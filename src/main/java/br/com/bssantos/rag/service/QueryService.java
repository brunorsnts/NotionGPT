package br.com.bssantos.rag.service;

import br.com.bssantos.rag.dto.ChatRequest;
import br.com.bssantos.rag.dto.ChatResponse;
import br.com.bssantos.rag.exception.FalhaNoProcessamentoException;
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
        try {
            return chatService.ask(request.query(), result);
        } catch (RuntimeException ex) {
            throw new FalhaNoProcessamentoException("Houve um problema na comunicação com a API da LLM");
        }
    }

    private List<EmbeddingMatch<TextSegment>> result(EmbeddingSearchRequest searchRequest) {
        try {
            EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
            return searchResult.matches();
        } catch (RuntimeException ex) {
            throw new FalhaNoProcessamentoException("Estamos enfrentando problema com a conexão com o banco de dados");
        }
    }

    private EmbeddingSearchRequest searchRequest(Embedding embedding) {
        return EmbeddingSearchRequest.builder()
                .queryEmbedding(embedding)
                .maxResults(4)
                .build();
    }

    private Embedding embed(ChatRequest request) {
        try {
            return embeddingModel.embed(request.query()).content();
        } catch (RuntimeException ex) {
            throw new FalhaNoProcessamentoException("Estamos enfrentando problemas com a API da CohereClient. Tente novamente mais tarde");
        }
    }
}
