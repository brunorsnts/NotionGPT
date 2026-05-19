package br.com.bssantos.rag.observability;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class MetricCollector {

    private final long startNanos;
    private int matchesCount;
    private List<Double> scores;
    private List<String> titulos;
    private final AtomicBoolean recorded;

    public MetricCollector(long startNanos) {
        this.startNanos = startNanos;
        this.matchesCount = 0;
        this.scores = List.of();
        this.titulos = List.of();
        this.recorded = new AtomicBoolean(false);
    }

    public void setMatchesCount(int count) {
        this.matchesCount = count;
    }

    public void setScores(List<Double> scores) {
        this.scores = scores;
    }

    public void setTitulos(List<String> titulos) {
        this.titulos = titulos;
    }

    public void flush(QueryMetricsService service, FailureStage stage) {
        if (!recorded.compareAndSet(false, true)) return;

        long latencyMs = (System.nanoTime() - startNanos) / 1_000_000;

        QueryMetric queryMetric = new QueryMetric(
                Instant.now(),
                latencyMs,
                matchesCount,
                scores,
                titulos,
                stage
        );
        service.record(queryMetric);
    }
}
