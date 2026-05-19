package br.com.bssantos.rag.service;

import br.com.bssantos.rag.dto.ChatRequest;
import br.com.bssantos.rag.dto.RetrievalResult;
import br.com.bssantos.rag.exception.FalhaNoProcessamentoException;
import br.com.bssantos.rag.observability.FailureStage;
import br.com.bssantos.rag.observability.QueryMetric;
import br.com.bssantos.rag.observability.QueryMetricsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.service.TokenStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryStreamServiceTest {

    @Mock
    private ContextRetrievalService contextRetrievalService;

    @Mock
    private StreamingChatService streamingChatService;

    @Mock
    private QueryMetricsService queryMetricsService;

    private QueryStreamService queryStreamService;

    @BeforeEach
    void setUp() {
        queryStreamService = new QueryStreamService(
                contextRetrievalService, streamingChatService, queryMetricsService, new ObjectMapper());
    }

    private RetrievalResult resultadoValido() {
        return new RetrievalResult("conteúdo de teste", 1, List.of(0.9), List.of("Título Teste"));
    }

    private void configurarContextoValido() {
        when(contextRetrievalService.buildContext(any(ChatRequest.class))).thenReturn(resultadoValido());
    }

    @SuppressWarnings("unchecked")
    private TokenStream tokenStreamQueEmiteToken(String token) {
        TokenStream tokenStream = mock(TokenStream.class);
        doAnswer(inv -> {
            Consumer<String> handler = inv.getArgument(0);
            handler.accept(token);
            return tokenStream;
        }).when(tokenStream).onPartialResponse(any());
        when(tokenStream.onCompleteResponse(any())).thenReturn(tokenStream);
        when(tokenStream.onError(any())).thenReturn(tokenStream);
        return tokenStream;
    }

    @SuppressWarnings("unchecked")
    private TokenStream tokenStreamQueCompleta() {
        TokenStream tokenStream = mock(TokenStream.class);
        when(tokenStream.onPartialResponse(any())).thenReturn(tokenStream);
        doAnswer(inv -> {
            Consumer<dev.langchain4j.model.chat.response.ChatResponse> handler = inv.getArgument(0);
            handler.accept(null);
            return tokenStream;
        }).when(tokenStream).onCompleteResponse(any());
        when(tokenStream.onError(any())).thenReturn(tokenStream);
        return tokenStream;
    }

    @SuppressWarnings("unchecked")
    private TokenStream tokenStreamQueErra(Throwable erro) {
        TokenStream tokenStream = mock(TokenStream.class);
        when(tokenStream.onPartialResponse(any())).thenReturn(tokenStream);
        when(tokenStream.onCompleteResponse(any())).thenReturn(tokenStream);
        doAnswer(inv -> {
            Consumer<Throwable> handler = inv.getArgument(0);
            handler.accept(erro);
            return tokenStream;
        }).when(tokenStream).onError(any());
        return tokenStream;
    }

    @Test
    void askStreaming_emiteEventoMeta_antesDosPrimeirosTokens() throws Exception {
        // Arrange
        configurarContextoValido();
        TokenStream tokenStream = mock(TokenStream.class);
        when(tokenStream.onPartialResponse(any())).thenReturn(tokenStream);
        when(tokenStream.onCompleteResponse(any())).thenReturn(tokenStream);
        when(tokenStream.onError(any())).thenReturn(tokenStream);
        when(streamingChatService.stream(any(ChatRequest.class), any(RetrievalResult.class), anyString())).thenReturn(tokenStream);
        SseEmitter emitter = mock(SseEmitter.class);

        // Act
        queryStreamService.askStreaming(new ChatRequest("pergunta válida"), "sessao-1", emitter);

        // Assert — meta deve ter sido enviado antes do stream começar
        InOrder ordem = inOrder(emitter, streamingChatService);
        ordem.verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        ordem.verify(streamingChatService).stream(any(ChatRequest.class), any(RetrievalResult.class), anyString());
    }

    @Test
    void askStreaming_emiteEventosToken_paraCadaChunk() throws Exception {
        // Arrange
        configurarContextoValido();
        TokenStream tokenStream = tokenStreamQueEmiteToken("chunk de texto");
        when(streamingChatService.stream(any(ChatRequest.class), any(RetrievalResult.class), anyString())).thenReturn(tokenStream);
        SseEmitter emitter = mock(SseEmitter.class);

        // Act
        queryStreamService.askStreaming(new ChatRequest("pergunta válida"), "sessao-1", emitter);

        // Assert — ao menos meta + token devem ter sido enviados
        verify(emitter, atLeast(2)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void askStreaming_emiteEventoDone_aoFinalizar() throws Exception {
        // Arrange
        configurarContextoValido();
        TokenStream tokenStream = tokenStreamQueCompleta();
        when(streamingChatService.stream(any(ChatRequest.class), any(RetrievalResult.class), anyString())).thenReturn(tokenStream);
        SseEmitter emitter = mock(SseEmitter.class);

        // Act
        queryStreamService.askStreaming(new ChatRequest("pergunta válida"), "sessao-1", emitter);

        // Assert — meta + done enviados, complete chamado
        verify(emitter, atLeast(2)).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter).complete();
    }

    @Test
    void askStreaming_gravaMétricaComNONE_noOnComplete() {
        // Arrange
        configurarContextoValido();
        TokenStream tokenStream = tokenStreamQueCompleta();
        when(streamingChatService.stream(any(ChatRequest.class), any(RetrievalResult.class), anyString())).thenReturn(tokenStream);
        SseEmitter emitter = mock(SseEmitter.class);

        // Act
        queryStreamService.askStreaming(new ChatRequest("pergunta válida"), "sessao-1", emitter);

        // Assert
        ArgumentCaptor<QueryMetric> metricCaptor = ArgumentCaptor.forClass(QueryMetric.class);
        verify(queryMetricsService).record(metricCaptor.capture());
        assertThat(metricCaptor.getValue().failureStage()).isEqualTo(FailureStage.NONE);
    }

    @Test
    void askStreaming_emiteEventoError_quandoLLMFalha() throws Exception {
        // Arrange
        configurarContextoValido();
        TokenStream tokenStream = tokenStreamQueErra(new RuntimeException("falha na LLM"));
        when(streamingChatService.stream(any(ChatRequest.class), any(RetrievalResult.class), anyString())).thenReturn(tokenStream);
        SseEmitter emitter = mock(SseEmitter.class);

        // Act
        queryStreamService.askStreaming(new ChatRequest("pergunta válida"), "sessao-1", emitter);

        // Assert — meta + error enviados, completeWithError chamado
        verify(emitter, atLeast(2)).send(any(SseEmitter.SseEventBuilder.class));
        verify(emitter).completeWithError(any(Throwable.class));
    }

    @Test
    void askStreaming_gravaMétricaComLLM_noOnError() {
        // Arrange
        configurarContextoValido();
        TokenStream tokenStream = tokenStreamQueErra(new RuntimeException("falha na LLM"));
        when(streamingChatService.stream(any(ChatRequest.class), any(RetrievalResult.class), anyString())).thenReturn(tokenStream);
        SseEmitter emitter = mock(SseEmitter.class);

        // Act
        queryStreamService.askStreaming(new ChatRequest("pergunta válida"), "sessao-1", emitter);

        // Assert
        ArgumentCaptor<QueryMetric> metricCaptor = ArgumentCaptor.forClass(QueryMetric.class);
        verify(queryMetricsService).record(metricCaptor.capture());
        assertThat(metricCaptor.getValue().failureStage()).isEqualTo(FailureStage.LLM);
    }

    @Test
    void askStreaming_completaComErro_quandoContextoFalha() {
        // Arrange
        when(contextRetrievalService.buildContext(any(ChatRequest.class)))
                .thenThrow(new FalhaNoProcessamentoException("erro embed", FailureStage.EMBED));
        SseEmitter emitter = mock(SseEmitter.class);

        // Act
        queryStreamService.askStreaming(new ChatRequest("pergunta válida"), "sessao-1", emitter);

        // Assert
        verify(emitter).completeWithError(any(FalhaNoProcessamentoException.class));
        verify(streamingChatService, never()).stream(any(ChatRequest.class), any(), anyString());
    }

    @Test
    void askStreaming_completaComErro_quandoContextoVazio() {
        // Arrange
        when(contextRetrievalService.buildContext(any(ChatRequest.class)))
                .thenThrow(new FalhaNoProcessamentoException("sem resultados", FailureStage.EMPTY));
        SseEmitter emitter = mock(SseEmitter.class);

        // Act
        queryStreamService.askStreaming(new ChatRequest("pergunta válida"), "sessao-1", emitter);

        // Assert
        verify(emitter).completeWithError(any(FalhaNoProcessamentoException.class));
        verify(streamingChatService, never()).stream(any(ChatRequest.class), any(), anyString());
    }

    @Test
    void askStreaming_enviaEventoError_antesDeCompleteWithError_quandoContextoFalha() throws Exception {
        // Arrange
        when(contextRetrievalService.buildContext(any(ChatRequest.class)))
                .thenThrow(new FalhaNoProcessamentoException("sem embedding", FailureStage.EMBED));
        SseEmitter emitter = mock(SseEmitter.class);

        // Act
        queryStreamService.askStreaming(new ChatRequest("pergunta"), "sessao-1", emitter);

        // Assert — evento error enviado antes do completeWithError
        InOrder ordem = inOrder(emitter);
        ordem.verify(emitter).send(any(SseEmitter.SseEventBuilder.class));
        ordem.verify(emitter).completeWithError(any(FalhaNoProcessamentoException.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void askStreaming_completaEmitterUmaVez_quandoOnCompleteEOnErrorDisparamJuntos() throws Exception {
        // Arrange — TokenStream que dispara onCompleteResponse e onError em sequência
        configurarContextoValido();
        TokenStream tokenStream = mock(TokenStream.class);
        when(tokenStream.onPartialResponse(any())).thenReturn(tokenStream);
        doAnswer(inv -> {
            Consumer<dev.langchain4j.model.chat.response.ChatResponse> handler = inv.getArgument(0);
            handler.accept(null);
            return tokenStream;
        }).when(tokenStream).onCompleteResponse(any());
        doAnswer(inv -> {
            Consumer<Throwable> handler = inv.getArgument(0);
            handler.accept(new RuntimeException("erro concorrente"));
            return tokenStream;
        }).when(tokenStream).onError(any());
        when(streamingChatService.stream(any(ChatRequest.class), any(RetrievalResult.class), anyString())).thenReturn(tokenStream);
        SseEmitter emitter = mock(SseEmitter.class);

        // Act
        queryStreamService.askStreaming(new ChatRequest("pergunta"), "sessao-1", emitter);

        // Assert — AtomicBoolean garante que apenas a primeira conclusão é aplicada
        verify(emitter, times(1)).complete();
        verify(emitter, never()).completeWithError(any(Throwable.class));
    }
}
