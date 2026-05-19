package br.com.bssantos.rag.service;

import br.com.bssantos.rag.dto.ChatRequest;
import br.com.bssantos.rag.dto.ChatResponse;
import br.com.bssantos.rag.dto.RetrievalResult;
import br.com.bssantos.rag.exception.FalhaNoProcessamentoException;
import br.com.bssantos.rag.observability.FailureStage;
import br.com.bssantos.rag.observability.QueryMetric;
import br.com.bssantos.rag.observability.QueryMetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryServiceTest {

    @Mock
    private ContextRetrievalService contextRetrievalService;

    @Mock
    private ChatService chatService;

    @Mock
    private QueryMetricsService queryMetricsService;

    private QueryService queryService;

    private static final ChatRequest REQUEST = new ChatRequest("o que é LangChain4J?");
    private static final String SESSION_ID = "session-abc";

    private static final RetrievalResult RETRIEVAL_RESULT = new RetrievalResult(
            "contexto relevante",
            2,
            List.of(0.9, 0.8),
            List.of("Aula 1", "Aula 2")
    );

    @BeforeEach
    void setUp() {
        queryService = new QueryService(contextRetrievalService, chatService, queryMetricsService);
    }

    @Test
    void retornaRespostaQuandoPipelineCompletoComSucesso() {
        // Arrange
        when(contextRetrievalService.buildContext(REQUEST)).thenReturn(RETRIEVAL_RESULT);
        when(chatService.ask(REQUEST.query(), RETRIEVAL_RESULT.context(), SESSION_ID))
                .thenReturn(new ChatResponse("resposta gerada"));

        // Act
        ChatResponse response = queryService.askIA(REQUEST, SESSION_ID);

        // Assert
        assertThat(response.reply()).isEqualTo("resposta gerada");
    }

    @Test
    void delegaBuildContextComORequestOriginal() {
        // Arrange
        when(contextRetrievalService.buildContext(REQUEST)).thenReturn(RETRIEVAL_RESULT);
        when(chatService.ask(anyString(), anyString(), anyString()))
                .thenReturn(new ChatResponse("qualquer"));

        // Act
        queryService.askIA(REQUEST, SESSION_ID);

        // Assert
        verify(contextRetrievalService).buildContext(REQUEST);
    }

    @Test
    void delegaParaChatServiceComContextoESessionIdCorretos() {
        // Arrange
        when(contextRetrievalService.buildContext(any())).thenReturn(RETRIEVAL_RESULT);
        when(chatService.ask(REQUEST.query(), RETRIEVAL_RESULT.context(), SESSION_ID))
                .thenReturn(new ChatResponse("resposta"));

        // Act
        queryService.askIA(REQUEST, SESSION_ID);

        // Assert
        verify(chatService).ask(REQUEST.query(), RETRIEVAL_RESULT.context(), SESSION_ID);
    }

    @Test
    void gravaMetricaComFailureStageNoneAposSucesso() {
        // Arrange
        when(contextRetrievalService.buildContext(any())).thenReturn(RETRIEVAL_RESULT);
        when(chatService.ask(anyString(), anyString(), anyString()))
                .thenReturn(new ChatResponse("ok"));
        ArgumentCaptor<QueryMetric> captor = ArgumentCaptor.forClass(QueryMetric.class);

        // Act
        queryService.askIA(REQUEST, SESSION_ID);

        // Assert
        verify(queryMetricsService).record(captor.capture());
        QueryMetric metric = captor.getValue();
        assertThat(metric.failureStage()).isEqualTo(FailureStage.NONE);
        assertThat(metric.matchesCount()).isEqualTo(2);
        assertThat(metric.scores()).containsExactly(0.9, 0.8);
        assertThat(metric.titulos()).containsExactly("Aula 1", "Aula 2");
        assertThat(metric.latencyMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void gravaMetricaComDadosDoRetrievalQuandoChatServiceFalha() {
        // Arrange
        when(contextRetrievalService.buildContext(any())).thenReturn(RETRIEVAL_RESULT);
        when(chatService.ask(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("timeout"));
        ArgumentCaptor<QueryMetric> captor = ArgumentCaptor.forClass(QueryMetric.class);

        // Act
        assertThatThrownBy(() -> queryService.askIA(REQUEST, SESSION_ID))
                .isInstanceOf(FalhaNoProcessamentoException.class);

        // Assert — retrievalResult != null, metrica deve ter os dados reais do retrieval
        verify(queryMetricsService).record(captor.capture());
        QueryMetric metric = captor.getValue();
        assertThat(metric.failureStage()).isEqualTo(FailureStage.LLM);
        assertThat(metric.matchesCount()).isEqualTo(2);
        assertThat(metric.scores()).containsExactly(0.9, 0.8);
    }

    @Test
    void gravaMetricaComZerosQuandoBuildContextFalha() {
        // Arrange
        when(contextRetrievalService.buildContext(any()))
                .thenThrow(new FalhaNoProcessamentoException("sem resultados", FailureStage.EMPTY));
        ArgumentCaptor<QueryMetric> captor = ArgumentCaptor.forClass(QueryMetric.class);

        // Act
        assertThatThrownBy(() -> queryService.askIA(REQUEST, SESSION_ID))
                .isInstanceOf(FalhaNoProcessamentoException.class);

        // Assert — retrievalResult == null, metrica deve ter zeros
        verify(queryMetricsService).record(captor.capture());
        QueryMetric metric = captor.getValue();
        assertThat(metric.failureStage()).isEqualTo(FailureStage.EMPTY);
        assertThat(metric.matchesCount()).isZero();
        assertThat(metric.scores()).isEmpty();
        assertThat(metric.titulos()).isEmpty();
    }

    @Test
    void propagaFailureStageDaExcecaoQuandoBuildContextLancaFalhaNoProcessamento() {
        // Arrange
        when(contextRetrievalService.buildContext(any()))
                .thenThrow(new FalhaNoProcessamentoException("sem embedding", FailureStage.EMBED));

        // Act & Assert
        assertThatThrownBy(() -> queryService.askIA(REQUEST, SESSION_ID))
                .isInstanceOf(FalhaNoProcessamentoException.class)
                .hasMessage("sem embedding")
                .extracting(e -> ((FalhaNoProcessamentoException) e).getFailureStage())
                .isEqualTo(FailureStage.EMBED);
    }

    @Test
    void propagaFailureStageEmptyQuandoBuildContextNaoEncontraResultados() {
        // Arrange
        when(contextRetrievalService.buildContext(any()))
                .thenThrow(new FalhaNoProcessamentoException(
                        "Nenhum conteúdo relevante encontrado nas suas anotações para responder essa pergunta",
                        FailureStage.EMPTY));

        // Act & Assert
        assertThatThrownBy(() -> queryService.askIA(REQUEST, SESSION_ID))
                .isInstanceOf(FalhaNoProcessamentoException.class)
                .extracting(e -> ((FalhaNoProcessamentoException) e).getFailureStage())
                .isEqualTo(FailureStage.EMPTY);
    }

    @Test
    void wrapsRuntimeExceptionDoChatServiceEmFalhaNoProcessamentoComStageLLM() {
        // Arrange
        when(contextRetrievalService.buildContext(any())).thenReturn(RETRIEVAL_RESULT);
        when(chatService.ask(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("conexão recusada"));

        // Act & Assert
        assertThatThrownBy(() -> queryService.askIA(REQUEST, SESSION_ID))
                .isInstanceOf(FalhaNoProcessamentoException.class)
                .hasMessage("Falha ao tentar conexão com LLM")
                .extracting(e -> ((FalhaNoProcessamentoException) e).getFailureStage())
                .isEqualTo(FailureStage.LLM);
    }

    @Test
    void gravaMetricaComFailureStageLlmQuandoChatServiceLancaRuntimeException() {
        // Arrange
        when(contextRetrievalService.buildContext(any())).thenReturn(RETRIEVAL_RESULT);
        when(chatService.ask(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("falha de rede"));
        ArgumentCaptor<QueryMetric> captor = ArgumentCaptor.forClass(QueryMetric.class);

        // Act
        assertThatThrownBy(() -> queryService.askIA(REQUEST, SESSION_ID))
                .isInstanceOf(FalhaNoProcessamentoException.class);

        // Assert
        verify(queryMetricsService).record(captor.capture());
        assertThat(captor.getValue().failureStage()).isEqualTo(FailureStage.LLM);
    }
}
