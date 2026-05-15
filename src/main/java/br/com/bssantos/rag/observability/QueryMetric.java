package br.com.bssantos.rag.observability;

import java.time.Instant;
import java.util.List;

public record QueryMetric(
        Instant timestamp,
        long latencyMs,
        int matchesCount,
        List<Double> scores,
        List<String> titulos,
        FailureStage failureStage
) {}
