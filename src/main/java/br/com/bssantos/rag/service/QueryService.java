package br.com.bssantos.rag.service;

import br.com.bssantos.rag.dto.ChatRequest;
import br.com.bssantos.rag.dto.ChatResponse;
import br.com.bssantos.rag.exception.FalhaNoProcessamentoException;
import br.com.bssantos.rag.observability.FailureStage;
import br.com.bssantos.rag.observability.QueryMetric;
import br.com.bssantos.rag.observability.QueryMetricsService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class QueryService {

    private final EmbeddingModel embeddingModel;
    private final ChatService chatService;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final QueryMetricsService queryMetricsService;

    public QueryService(@Qualifier("embeddingQuery") EmbeddingModel embeddingModel,
                        ChatService chatService,
                        EmbeddingStore<TextSegment> embeddingStore,
                        QueryMetricsService queryMetricsService) {
        this.embeddingModel = embeddingModel;
        this.chatService = chatService;
        this.embeddingStore = embeddingStore;
        this.queryMetricsService = queryMetricsService;
    }

    public ChatResponse askIA(ChatRequest request, String sessionId) {
        long startNanos = System.nanoTime();
        int matchesCount = 0;
        List<Double> scores = List.of();
        List<String> titulos = List.of();
        FailureStage failureStage = FailureStage.LLM;

        try {
            failureStage = FailureStage.EMBED;
            Embedding embedding = embed(request);

            failureStage = FailureStage.SEARCH;
            List<EmbeddingMatch<TextSegment>> result = result(searchRequest(embedding));
            matchesCount = result.size();
            scores = result.stream().map(EmbeddingMatch::score).toList();
            titulos = result.stream()
                    .map(m -> m.embedded().metadata().getString("titulo")).toList();

            if (result.isEmpty()) {
                failureStage = FailureStage.EMPTY;
                throw new FalhaNoProcessamentoException("Nenhum conteúdo relevante encontrado nas suas anotações para responder essa pergunta");
            }
            try {
                ChatResponse response = chatService.ask(request.query(), result, sessionId);
                failureStage = FailureStage.NONE;
                return response;
            } catch (RuntimeException ex) {
                throw new FalhaNoProcessamentoException("Houve um problema na comunicação com a API da LLM", ex);
            }

        } finally {
            QueryMetric queryMetric = new QueryMetric(Instant.now(),
                    (System.nanoTime() - startNanos) / 1_000_000,
                    matchesCount,
                    scores,
                    titulos,
                    failureStage
            );
            queryMetricsService.record(queryMetric);
        }

    }

    private List<EmbeddingMatch<TextSegment>> result(EmbeddingSearchRequest searchRequest) {
        try {
            EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
            return searchResult.matches();
        } catch (RuntimeException ex) {
            if (ex instanceof FalhaNoProcessamentoException) throw ex;
            throw new FalhaNoProcessamentoException("Estamos enfrentando problema com a conexão com o banco de dados");
        }
    }

    private EmbeddingSearchRequest searchRequest(Embedding embedding) {
        return EmbeddingSearchRequest.builder()
                .queryEmbedding(embedding)
                .maxResults(6)
                .minScore(0.5)
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
