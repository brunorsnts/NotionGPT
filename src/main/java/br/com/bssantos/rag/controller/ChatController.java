package br.com.bssantos.rag.controller;

import br.com.bssantos.rag.dto.ChatRequest;
import br.com.bssantos.rag.dto.ChatResponse;
import br.com.bssantos.rag.service.QueryService;
import br.com.bssantos.rag.service.QueryStreamService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
public class ChatController {

    private final QueryService queryService;
    private final QueryStreamService queryStreamService;

    public ChatController(QueryService queryService, QueryStreamService queryStreamService) {
        this.queryService = queryService;
        this.queryStreamService = queryStreamService;
    }

    @PostMapping("/query")
    public ResponseEntity<ChatResponse> realizaConsulta(@RequestBody @Valid ChatRequest request,
                                                        @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }
        ChatResponse response = queryService.askIA(request, sessionId);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/query/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamConsulta(@RequestBody @Valid ChatRequest request,
                                     @RequestHeader(value = "X-Session-Id", required = false) String sessionId) {

        if (sessionId == null || sessionId.isBlank()) {
            sessionId = UUID.randomUUID().toString();
        }

        SseEmitter sseEmitter = new SseEmitter(0L);
        sseEmitter.onTimeout(sseEmitter::complete);
        queryStreamService.askStreaming(request, sessionId, sseEmitter);
        return sseEmitter;
    }
}
