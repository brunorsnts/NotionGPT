package br.com.bssantos.rag.observability;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class QueryMetricsService {

    static final int MAX_HISTORY = 100;
    static final int TOP_PAGES = 5;

    private final Deque<QueryMetric> history = new ArrayDeque<>(MAX_HISTORY);

    public synchronized void record(QueryMetric metric) {
        if (history.size() == MAX_HISTORY) {
            history.removeFirst();
        }
        history.addLast(metric);
    }

    public synchronized StatsResponse getStats() {
        List<QueryMetric> snapshot = new ArrayList<>(history);

        int totalQueries = snapshot.size();

        long successCount = snapshot.stream()
                .filter(m -> m.failureStage() == FailureStage.NONE)
                .count();

        long failureCount = totalQueries - successCount;

        double avgLatencyMs = snapshot.stream()
                .mapToLong(QueryMetric::latencyMs)
                .average()
                .orElse(0.0);

        double avgMatchesCount = snapshot.stream()
                .mapToInt(QueryMetric::matchesCount)
                .average()
                .orElse(0.0);

        double avgScore = snapshot.stream()
                .flatMap(m -> m.scores().stream())
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        long p95LatencyMs = 0L;

        if (!snapshot.isEmpty()) {
            List<Long> latencias = snapshot.stream()
                    .map(QueryMetric::latencyMs)
                    .sorted()
                    .toList();
            int index = (int) Math.ceil(0.95 * latencias.size()) - 1;
            p95LatencyMs = latencias.get(index);
        }

        List<StatsResponse.TopPage> topPages = snapshot.stream()
                .flatMap(m -> m.titulos().stream())
                .filter(t -> t != null && !t.isBlank())
                .collect(Collectors.groupingBy(t -> t, Collectors.counting()))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(TOP_PAGES)
                .map(e -> new StatsResponse.TopPage(e.getKey(), e.getValue()))
                .toList();

        return new StatsResponse(totalQueries,
                successCount,
                failureCount,
                avgLatencyMs,
                p95LatencyMs,
                avgMatchesCount,
                avgScore,
                topPages,
                totalQueries,
                MAX_HISTORY);
    }
}
