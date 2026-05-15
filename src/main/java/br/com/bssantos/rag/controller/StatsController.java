package br.com.bssantos.rag.controller;

import br.com.bssantos.rag.observability.QueryMetricsService;
import br.com.bssantos.rag.observability.StatsResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stats")
public class StatsController {

    private final QueryMetricsService service;

    public StatsController(QueryMetricsService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<StatsResponse> stats() {
        return ResponseEntity.ok(service.getStats());
    }
}
