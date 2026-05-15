package br.com.bssantos.rag.observability;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QueryMetricsServiceTest {

    private QueryMetricsService service;

    @BeforeEach
    void setUp() {
        service = new QueryMetricsService();
    }

    private QueryMetric metricaSucesso(long latencyMs, int matches, List<Double> scores, List<String> titulos) {
        return new QueryMetric(Instant.now(), latencyMs, matches, scores, titulos, FailureStage.NONE);
    }

    private QueryMetric metricaFalha(FailureStage stage) {
        return new QueryMetric(Instant.now(), 50L, 0, List.of(), List.of(), stage);
    }

    @Test
    @DisplayName("getStats() com histórico vazio retorna zeros e listas vazias, sem NPE")
    void getStats_historicoVazio_retornaZeros() {
        StatsResponse stats = service.getStats();

        assertNotNull(stats);
        assertEquals(0L, stats.totalQueries());
        assertEquals(0L, stats.successCount());
        assertEquals(0L, stats.failureCount());
        assertEquals(0.0, stats.avgLatencyMs());
        assertEquals(0L, stats.p95LatencyMs());
        assertEquals(0.0, stats.avgMatchesCount());
        assertEquals(0.0, stats.avgScore());
        assertNotNull(stats.topPages());
        assertTrue(stats.topPages().isEmpty());
        assertEquals(0L, stats.historySize());
        assertEquals(QueryMetricsService.MAX_HISTORY, stats.historyCapacity());
    }

    @Test
    @DisplayName("Registrar 1 métrica de sucesso incrementa totalQueries e successCount")
    void record_umaSucesso_contadoresCorretos() {
        service.record(metricaSucesso(100L, 3, List.of(0.9, 0.8, 0.7), List.of("A", "B", "C")));

        StatsResponse stats = service.getStats();

        assertEquals(1L, stats.totalQueries());
        assertEquals(1L, stats.successCount());
        assertEquals(0L, stats.failureCount());
    }

    @Test
    @DisplayName("Registrar 1 métrica com failureStage=EMPTY incrementa failureCount")
    void record_umaFalha_failureCountIncrementado() {
        service.record(metricaFalha(FailureStage.EMPTY));

        StatsResponse stats = service.getStats();

        assertEquals(1L, stats.totalQueries());
        assertEquals(0L, stats.successCount());
        assertEquals(1L, stats.failureCount());
    }

    @Test
    @DisplayName("avgLatencyMs é a média aritmética das latências registradas")
    void getStats_avgLatencyMs_mediaAritmeticaCorreta() {
        service.record(metricaSucesso(100L, 1, List.of(0.9), List.of("A")));
        service.record(metricaSucesso(200L, 1, List.of(0.8), List.of("B")));
        service.record(metricaSucesso(300L, 1, List.of(0.7), List.of("C")));

        assertEquals(200.0, service.getStats().avgLatencyMs(), 0.001);
    }

    @Test
    @DisplayName("avgScore é calculada sobre todos os scores achatados de todas as métricas")
    void getStats_avgScore_achatadoDeTodasMetricas() {
        service.record(metricaSucesso(100L, 2, List.of(0.9, 0.8), List.of("A", "B")));
        service.record(metricaSucesso(150L, 1, List.of(0.5), List.of("C")));

        assertEquals(0.7333, service.getStats().avgScore(), 0.001);
    }

    @Test
    @DisplayName("avgMatchesCount é a média do número de matches por métrica")
    void getStats_avgMatchesCount_mediaCorreta() {
        service.record(metricaSucesso(100L, 4, List.of(0.9, 0.8, 0.7, 0.6), List.of("A", "B", "C", "D")));
        service.record(metricaSucesso(100L, 2, List.of(0.9, 0.8), List.of("E", "F")));

        assertEquals(3.0, service.getStats().avgMatchesCount(), 0.001);
    }

    @Test
    @DisplayName("p95LatencyMs é o percentil 95 das latências registradas")
    void getStats_p95LatencyMs_percentil95Correto() {
        for (int i = 1; i <= 20; i++) {
            service.record(metricaSucesso(i * 10L, 1, List.of(0.8), List.of("Página")));
        }

        long p95 = service.getStats().p95LatencyMs();
        assertTrue(p95 >= 180L && p95 <= 200L, "p95 deve estar entre 180 e 200, foi: " + p95);
    }

    @Test
    @DisplayName("Registrar MAX_HISTORY+1 métricas mantém historySize em MAX_HISTORY")
    void record_maisQueMaxHistory_historySizePermaneceMaxHistory() {
        for (int i = 0; i < QueryMetricsService.MAX_HISTORY + 1; i++) {
            service.record(metricaSucesso(100L, 1, List.of(0.8), List.of("Página")));
        }

        assertEquals(QueryMetricsService.MAX_HISTORY, service.getStats().historySize());
    }

    @Test
    @DisplayName("Ao exceder MAX_HISTORY, a métrica mais antiga é descartada")
    void record_overflow_descartaMetricaMaisAntiga() {
        for (int i = 0; i < QueryMetricsService.MAX_HISTORY; i++) {
            service.record(metricaSucesso(10L, 1, List.of(0.8), List.of("Página")));
        }
        service.record(metricaSucesso(1000L, 1, List.of(0.8), List.of("Página")));

        double expected = (10.0 * (QueryMetricsService.MAX_HISTORY - 1) + 1000.0) / QueryMetricsService.MAX_HISTORY;
        assertEquals(expected, service.getStats().avgLatencyMs(), 0.001);
    }

    @Test
    @DisplayName("topPages ordena páginas pela frequência de aparição (count decrescente)")
    void getStats_topPages_paginaMaisFrequenteTemCountMaior() {
        service.record(metricaSucesso(100L, 2, List.of(0.9, 0.8), List.of("Página A", "Página B")));
        service.record(metricaSucesso(100L, 1, List.of(0.9), List.of("Página A")));
        service.record(metricaSucesso(100L, 1, List.of(0.9), List.of("Página A")));

        StatsResponse.TopPage primeira = service.getStats().topPages().get(0);
        assertEquals("Página A", primeira.titulo());
        assertEquals(3L, primeira.count());
    }

    @Test
    @DisplayName("topPages é limitado a TOP_PAGES mesmo com mais títulos distintos")
    void getStats_topPages_limitadoATopPages() {
        for (int i = 1; i <= QueryMetricsService.TOP_PAGES + 3; i++) {
            service.record(metricaSucesso(100L, 1, List.of(0.8), List.of("Página " + i)));
        }

        assertTrue(service.getStats().topPages().size() <= QueryMetricsService.TOP_PAGES);
    }

    @Test
    @DisplayName("Títulos blank são ignorados no ranking de topPages")
    void getStats_topPages_ignoraTitulosBlank() {
        service.record(metricaSucesso(100L, 3, List.of(0.9, 0.8, 0.7), List.of("Página Válida", "", "   ")));

        boolean contemInvalido = service.getStats().topPages().stream()
                .anyMatch(p -> p.titulo() == null || p.titulo().isBlank());
        assertFalse(contemInvalido);
    }

    @Test
    @DisplayName("Título null na lista de títulos é ignorado no ranking de topPages")
    void getStats_topPages_ignoraTituloNull() {
        List<String> titulosComNull = new ArrayList<>();
        titulosComNull.add("Página Válida");
        titulosComNull.add(null);

        service.record(new QueryMetric(Instant.now(), 100L, 2, List.of(0.9, 0.8), titulosComNull, FailureStage.NONE));

        boolean contemNull = service.getStats().topPages().stream()
                .anyMatch(p -> p.titulo() == null);
        assertFalse(contemNull);
    }

    @Test
    @DisplayName("historyCapacity sempre retorna MAX_HISTORY independente do estado")
    void getStats_historyCapacity_sempreMaxHistory() {
        assertEquals(QueryMetricsService.MAX_HISTORY, service.getStats().historyCapacity());
        service.record(metricaSucesso(100L, 1, List.of(0.8), List.of("A")));
        assertEquals(QueryMetricsService.MAX_HISTORY, service.getStats().historyCapacity());
    }

    @Test
    @DisplayName("Métricas com scores vazios não causam exceção nem NaN em avgScore")
    void getStats_scoresVazios_naoQuebramAvgScore() {
        service.record(new QueryMetric(Instant.now(), 50L, 0, List.of(), List.of(), FailureStage.EMPTY));

        StatsResponse stats = assertDoesNotThrow(() -> service.getStats());
        assertFalse(Double.isNaN(stats.avgScore()));
        assertEquals(0.0, stats.avgScore(), 0.001);
    }

    @Test
    @DisplayName("Múltiplos estágios de falha distintos são todos contabilizados em failureCount")
    void record_multiplosEstagiosDeFalha_todosContabilizados() {
        service.record(metricaFalha(FailureStage.EMBED));
        service.record(metricaFalha(FailureStage.SEARCH));
        service.record(metricaFalha(FailureStage.EMPTY));
        service.record(metricaFalha(FailureStage.LLM));
        service.record(metricaSucesso(100L, 1, List.of(0.9), List.of("A")));

        StatsResponse stats = service.getStats();
        assertEquals(5L, stats.totalQueries());
        assertEquals(1L, stats.successCount());
        assertEquals(4L, stats.failureCount());
    }
}
