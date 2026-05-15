package br.com.bssantos.rag.observability;

import java.util.List;

public record StatsResponse(
        long totalQueries,
        long successCount,
        long failureCount,
        double avgLatencyMs,
        long p95LatencyMs,
        double avgMatchesCount,
        double avgScore,
        List<TopPage> topPages,
        long historySize,
        int historyCapacity
) {
    public record TopPage(String titulo, long count) {}
}
