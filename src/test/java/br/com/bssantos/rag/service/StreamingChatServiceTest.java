package br.com.bssantos.rag.service;

import br.com.bssantos.rag.dto.ChatRequest;
import br.com.bssantos.rag.dto.RetrievalResult;
import dev.langchain4j.service.TokenStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StreamingChatServiceTest {

    @Mock
    private StreamingChatAssistant streamingChatAssistant;

    private StreamingChatService streamingChatService;

    @BeforeEach
    void setUp() {
        streamingChatService = new StreamingChatService(streamingChatAssistant);
    }

    @Test
    void stream_retornaTokenStream_quandoRetrievalResultValido() {
        // Arrange
        TokenStream tokenStream = mock(TokenStream.class);
        when(streamingChatAssistant.chat(anyString(), anyString(), anyString())).thenReturn(tokenStream);
        RetrievalResult retrieval = new RetrievalResult("contexto relevante", 1, List.of(0.9), List.of("Título"));

        // Act
        TokenStream resultado = streamingChatService.stream(new ChatRequest("minha pergunta"), retrieval, "sessao-1");

        // Assert
        assertThat(resultado).isNotNull();
        assertThat(resultado).isSameAs(tokenStream);
    }

    @Test
    void stream_passaContextoDoRetrievalParaOAssistant() {
        // Arrange
        TokenStream tokenStream = mock(TokenStream.class);
        when(streamingChatAssistant.chat(anyString(), anyString(), anyString())).thenReturn(tokenStream);
        RetrievalResult retrieval = new RetrievalResult("contexto construído", 1, List.of(0.9), List.of("Título"));

        // Act
        streamingChatService.stream(new ChatRequest("pergunta?"), retrieval, "sessao-1");

        // Assert
        ArgumentCaptor<String> contextoCaptor = ArgumentCaptor.forClass(String.class);
        verify(streamingChatAssistant).chat(eq("sessao-1"), contextoCaptor.capture(), eq("pergunta?"));
        assertThat(contextoCaptor.getValue()).isEqualTo("contexto construído");
    }
}
