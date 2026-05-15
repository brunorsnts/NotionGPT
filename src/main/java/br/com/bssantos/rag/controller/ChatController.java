package br.com.bssantos.rag.controller;

import br.com.bssantos.rag.dto.ChatRequest;
import br.com.bssantos.rag.dto.ChatResponse;
import br.com.bssantos.rag.service.QueryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
public class ChatController {

    private final QueryService queryService;

    public ChatController(QueryService queryService) {
        this.queryService = queryService;
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
}
