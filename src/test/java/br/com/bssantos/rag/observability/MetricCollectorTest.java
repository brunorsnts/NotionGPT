package br.com.bssantos.rag.observability;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MetricCollectorTest {

    @Mock
    private QueryMetricsService queryMetricsService;

    @Test
    void flush_gravaMétricaCorretamente_comTodosOsCampos() {
        // Arrange
        long startNanos = System.nanoTime();
        MetricCollector collector = new MetricCollector(startNanos);
        collector.setMatchesCount(3);
        collector.setScores(List.of(0.9, 0.8, 0.7));
        collector.setTitulos(List.of("Título A", "Título B", "Título C"));

        // Act
        collector.flush(queryMetricsService, FailureStage.NONE);

        // Assert
        ArgumentCaptor<QueryMetric> captor = ArgumentCaptor.forClass(QueryMetric.class);
        verify(queryMetricsService).record(captor.capture());
        QueryMetric metrica = captor.getValue();

        assertThat(metrica.matchesCount()).isEqualTo(3);
        assertThat(metrica.scores()).containsExactly(0.9, 0.8, 0.7);
        assertThat(metrica.titulos()).containsExactly("Título A", "Título B", "Título C");
        assertThat(metrica.failureStage()).isEqualTo(FailureStage.NONE);
        assertThat(metrica.timestamp()).isNotNull();
    }

    @Test
    void flush_garanteExecucaoUnica_mesmoChamadoDuasVezes() {
        // Arrange
        long startNanos = System.nanoTime();
        MetricCollector collector = new MetricCollector(startNanos);
        collector.setMatchesCount(1);
        collector.setScores(List.of(0.85));
        collector.setTitulos(List.of("Título X"));

        // Act
        collector.flush(queryMetricsService, FailureStage.NONE);
        collector.flush(queryMetricsService, FailureStage.LLM);

        // Assert — record deve ser chamado apenas uma vez
        verify(queryMetricsService, times(1)).record(any());
    }

    @Test
    void flush_calculaLatenciaCorretamente() {
        // Arrange
        long startNanos = System.nanoTime() - 50_000_000L; // simula 50ms decorridos
        MetricCollector collector = new MetricCollector(startNanos);
        collector.setMatchesCount(0);
        collector.setScores(List.of());
        collector.setTitulos(List.of());

        // Act
        collector.flush(queryMetricsService, FailureStage.EMBED);

        // Assert
        ArgumentCaptor<QueryMetric> captor = ArgumentCaptor.forClass(QueryMetric.class);
        verify(queryMetricsService).record(captor.capture());
        assertThat(captor.getValue().latencyMs()).isGreaterThanOrEqualTo(50L);
    }

    private static <T> T any() {
        return org.mockito.ArgumentMatchers.any();
    }
}
