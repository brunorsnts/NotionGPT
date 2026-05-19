package br.com.bssantos.rag.service;

import br.com.bssantos.rag.dto.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatAssistant chatAssistant;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(chatAssistant);
    }

    @Test
    void retornaRespostaDoChatAssistant() {
        // Arrange
        when(chatAssistant.chat(anyString(), anyString(), anyString())).thenReturn("resposta gerada pelo modelo");

        // Act
        ChatResponse response = chatService.ask("minha pergunta", "contexto relevante", "test-session");

        // Assert
        assertThat(response.reply()).isEqualTo("resposta gerada pelo modelo");
    }

    @Test
    void delegaParaChatAssistantComArgumentosCorretos() {
        // Arrange
        when(chatAssistant.chat(anyString(), anyString(), anyString())).thenReturn("qualquer");

        // Act
        chatService.ask("minha pergunta", "contexto de teste", "sessao-123");

        // Assert
        verify(chatAssistant).chat("sessao-123", "contexto de teste", "minha pergunta");
    }

    @Test
    void retornaRespostaEnvelopandoStringDoChatAssistant() {
        // Arrange
        when(chatAssistant.chat(anyString(), anyString(), anyString())).thenReturn("resposta específica");

        // Act
        ChatResponse response = chatService.ask("pergunta?", "ctx", "sessao");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.reply()).isEqualTo("resposta específica");
    }
}
