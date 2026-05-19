package br.com.bssantos.rag.service;

import br.com.bssantos.rag.dto.ChatRequest;
import br.com.bssantos.rag.dto.ChatResponse;
import br.com.bssantos.rag.dto.RetrievalResult;
import br.com.bssantos.rag.exception.FalhaNoProcessamentoException;
import br.com.bssantos.rag.observability.FailureStage;
import br.com.bssantos.rag.observability.QueryMetric;
import br.com.bssantos.rag.observability.QueryMetricsService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class QueryService {

    private final ContextRetrievalService contextRetrievalService;
    private final ChatService chatService;
    private final QueryMetricsService queryMetricsService;

    public QueryService(ContextRetrievalService contextRetrievalService,
                        ChatService chatService,
                        QueryMetricsService queryMetricsService) {
        this.contextRetrievalService = contextRetrievalService;
        this.chatService = chatService;
        this.queryMetricsService = queryMetricsService;
    }

    public ChatResponse askIA(ChatRequest request, String sessionId) {
        long startNanos = System.nanoTime();
        RetrievalResult retrievalResult = null;
        FailureStage failureStage = FailureStage.LLM;
        try {
            retrievalResult = contextRetrievalService.buildContext(request);
            ChatResponse response = chatService.ask(request.query(), retrievalResult.context(), sessionId);
            failureStage = FailureStage.NONE;
            return response;

        } catch (FalhaNoProcessamentoException ex) {
            failureStage = ex.getFailureStage();
            throw ex;

        } catch (RuntimeException ex) {
            throw new FalhaNoProcessamentoException("Falha ao tentar conexão com LLM", FailureStage.LLM);

        } finally {
            QueryMetric queryMetric;
            if (retrievalResult != null) {
                queryMetric = new QueryMetric(Instant.now(),
                        (System.nanoTime() - startNanos) / 1_000_000,
                        retrievalResult.matchesCount(),
                        retrievalResult.scores(),
                        retrievalResult.titulos(),
                        failureStage
                );
            } else {
                queryMetric = new QueryMetric(Instant.now(),
                        (System.nanoTime() - startNanos) / 1_000_000,
                        0,
                        List.of(),
                        List.of(),
                        failureStage);
            }
            queryMetricsService.record(queryMetric);
        }
    }
}
