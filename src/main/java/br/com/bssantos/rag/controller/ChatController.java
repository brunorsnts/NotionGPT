package br.com.bssantos.rag.controller;

import br.com.bssantos.rag.dto.ChatRequest;
import br.com.bssantos.rag.dto.ChatResponse;
import br.com.bssantos.rag.service.QueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

    private final QueryService queryService;

    public ChatController(QueryService queryService) {
        this.queryService = queryService;
    }

    @PostMapping("/query")
    public ResponseEntity<ChatResponse> realizaConsulta(@RequestBody ChatRequest request) {
        ChatResponse response = queryService.askIA(request);
        return ResponseEntity.ok(response);
    }
}
