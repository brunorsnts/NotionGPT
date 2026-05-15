package br.com.bssantos.rag.service;

import br.com.bssantos.rag.dto.ChatResponse;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
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

    private EmbeddingMatch<TextSegment> matchWith(String text) {
        Embedding dummyEmbedding = Embedding.from(new float[]{0.1f, 0.2f});
        TextSegment segment = TextSegment.from(text);
        return new EmbeddingMatch<>(0.9, "id-" + text.hashCode(), dummyEmbedding, segment);
    }

    @Test
    void promptContemCabecalhoDeAssistente() {
        // Arrange
        when(chatAssistant.chat(anyString(), anyString(), anyString())).thenReturn("resposta qualquer");
        List<EmbeddingMatch<TextSegment>> matches = List.of(matchWith("conteudo de teste"));

        // Act
        ChatResponse response = chatService.ask("minha pergunta", matches, "test-session");

        // Assert
        assertThat(response.reply()).isEqualTo("resposta qualquer");
    }

    @Test
    void promptContemTextoDoContextoDosMatches() {
        // Arrange
        when(chatAssistant.chat(anyString(), anyString(), anyString())).thenReturn("resposta qualquer");
        List<EmbeddingMatch<TextSegment>> matches = List.of(matchWith("trecho relevante do contexto"));

        // Act
        ChatResponse response = chatService.ask("qual é a pergunta?", matches, "test-session");

        // Assert
        assertThat(response.reply()).isEqualTo("resposta qualquer");
    }

    @Test
    void promptContemDelimitadorDePergunta() {
        // Arrange
        when(chatAssistant.chat(anyString(), anyString(), anyString())).thenReturn("resposta qualquer");
        List<EmbeddingMatch<TextSegment>> matches = List.of(matchWith("qualquer contexto"));

        // Act
        ChatResponse response = chatService.ask("qual é a pergunta?", matches, "test-session");

        // Assert
        assertThat(response.reply()).isEqualTo("resposta qualquer");
    }

    @Test
    void promptSeparaMultiplosMatchesPorDuplaQuebraLinha() {
        // Arrange
        when(chatAssistant.chat(anyString(), anyString(), anyString())).thenReturn("resposta qualquer");
        List<EmbeddingMatch<TextSegment>> matches = List.of(
                matchWith("primeiro trecho"),
                matchWith("segundo trecho")
        );

        // Act
        ChatResponse response = chatService.ask("pergunta sobre os trechos", matches, "test-session");

        // Assert
        assertThat(response.reply()).isEqualTo("resposta qualquer");
    }

    @Test
    void retornaRespostaDoChatModel() {
        // Arrange
        when(chatAssistant.chat(anyString(), anyString(), anyString())).thenReturn("resposta gerada pelo modelo");
        List<EmbeddingMatch<TextSegment>> matches = List.of(matchWith("contexto"));

        // Act
        ChatResponse response = chatService.ask("pergunta?", matches, "test-session");

        // Assert
        assertThat(response.reply()).isEqualTo("resposta gerada pelo modelo");
    }
}
