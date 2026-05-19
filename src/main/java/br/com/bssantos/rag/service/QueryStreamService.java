package br.com.bssantos.rag.service;

import br.com.bssantos.rag.dto.ChatRequest;
import br.com.bssantos.rag.dto.RetrievalResult;
import br.com.bssantos.rag.exception.FalhaNoProcessamentoException;
import br.com.bssantos.rag.observability.FailureStage;
import br.com.bssantos.rag.observability.MetricCollector;
import br.com.bssantos.rag.observability.QueryMetricsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class QueryStreamService {

    private static final Logger log = LoggerFactory.getLogger(QueryStreamService.class);

    private final ContextRetrievalService contextRetrievalService;
    private final StreamingChatService streamingChatService;
    private final QueryMetricsService queryMetricsService;
    private final ObjectMapper objectMapper;

    public QueryStreamService(ContextRetrievalService contextRetrievalService,
                              StreamingChatService streamingChatService,
                              QueryMetricsService queryMetricsService,
                              ObjectMapper objectMapper) {
        this.contextRetrievalService = contextRetrievalService;
        this.streamingChatService = streamingChatService;
        this.queryMetricsService = queryMetricsService;
        this.objectMapper = objectMapper;
    }

    public SseEmitter askStreaming(ChatRequest request, String sessionId, SseEmitter emitter) {
        MetricCollector metricCollector = new MetricCollector(System.nanoTime());
        AtomicBoolean emitterDone = new AtomicBoolean(false);
        RetrievalResult retrievalResult = null;

        try {
            retrievalResult = contextRetrievalService.buildContext(request);
            metricCollector.setMatchesCount(retrievalResult.matchesCount());
            metricCollector.setScores(retrievalResult.scores());
            metricCollector.setTitulos(retrievalResult.titulos());

            try {
                emitter.send(SseEmitter.event().name("meta").data(retrievalResult.matchesCount()));
            } catch (IOException metaError) {
                metricCollector.flush(queryMetricsService, FailureStage.NONE);
                try { emitter.send(SseEmitter.event().name("error").data(metaError.getMessage())); } catch (IOException ignored) {}
                emitter.completeWithError(metaError);
                return emitter;
            }

            streamingChatService.stream(request, retrievalResult, sessionId)
                    .onPartialResponse(token -> {
                        if (emitterDone.get()) return;
                        try {
                            emitter.send(SseEmitter.event().name("token").data(objectMapper.writeValueAsString(token)));
                        } catch (Exception e) {
                            if (emitterDone.compareAndSet(false, true)) {
                                emitter.completeWithError(e);
                            }
                        }
                    })
                    .onCompleteResponse(done -> {
                        metricCollector.flush(queryMetricsService, FailureStage.NONE);
                        try {
                            emitter.send(SseEmitter.event().name("done").data(""));
                        } catch (IOException e) {
                            log.warn("Failed to send 'done' SSE event for session {}: {}", sessionId, e.getMessage());
                        }
                        if (emitterDone.compareAndSet(false, true)) {
                            emitter.complete();
                        }
                    })
                    .onError(error -> {
                        log.error("LLM streaming error for session {}", sessionId, error);
                        metricCollector.flush(queryMetricsService, FailureStage.LLM);
                        try {
                            emitter.send(SseEmitter.event().name("error").data(error.getMessage()));
                        } catch (IOException e) {
                            log.warn("Failed to send SSE error event for session {}: {}", sessionId, e.getMessage());
                        }
                        if (emitterDone.compareAndSet(false, true)) {
                            emitter.completeWithError(error);
                        }
                    })
                    .start();
            return emitter;
        } catch (FalhaNoProcessamentoException ex) {
            metricCollector.flush(queryMetricsService, ex.getFailureStage());
            try { emitter.send(SseEmitter.event().name("error").data(ex.getMessage())); } catch (IOException ignored) {}
            emitter.completeWithError(ex);
            return emitter;
        } catch (RuntimeException ex) {
            metricCollector.flush(queryMetricsService, FailureStage.EMBED);
            try { emitter.send(SseEmitter.event().name("error").data(ex.getMessage())); } catch (IOException ignored) {}
            emitter.completeWithError(ex);
            return emitter;
        }
    }
}
