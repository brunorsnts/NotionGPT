package br.com.bssantos.rag.service;

import br.com.bssantos.rag.dto.ChatResponse;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ChatModel chatModel;

    private ChatService chatService;

    @BeforeEach
    void setUp() {
        chatService = new ChatService(chatModel);
    }

    private EmbeddingMatch<TextSegment> matchWith(String text) {
        Embedding dummyEmbedding = Embedding.from(new float[]{0.1f, 0.2f});
        TextSegment segment = TextSegment.from(text);
        return new EmbeddingMatch<>(0.9, "id-" + text.hashCode(), dummyEmbedding, segment);
    }

    @Test
    void promptContemCabecalhoDeAssistente() {
        // Arrange
        when(chatModel.chat(anyString())).thenReturn("resposta qualquer");
        List<EmbeddingMatch<TextSegment>> matches = List.of(matchWith("conteudo de teste"));
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        chatService.ask("minha pergunta", matches);

        // Assert
        verify(chatModel).chat(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("Você é um assistente que responde perguntas exclusivamente com base no contexto fornecido abaixo.");
    }

    @Test
    void promptContemTextoDoContextoDosMatches() {
        // Arrange
        when(chatModel.chat(anyString())).thenReturn("resposta qualquer");
        List<EmbeddingMatch<TextSegment>> matches = List.of(matchWith("trecho relevante do contexto"));
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        chatService.ask("qual é a pergunta?", matches);

        // Assert
        verify(chatModel).chat(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("trecho relevante do contexto");
    }

    @Test
    void promptContemDelimitadorDePergunta() {
        // Arrange
        when(chatModel.chat(anyString())).thenReturn("resposta qualquer");
        List<EmbeddingMatch<TextSegment>> matches = List.of(matchWith("qualquer contexto"));
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        chatService.ask("qual é a pergunta?", matches);

        // Assert
        verify(chatModel).chat(promptCaptor.capture());
        String prompt = promptCaptor.getValue();
        assertThat(prompt).contains("<pergunta>");
        assertThat(prompt).contains("</pergunta>");
        assertThat(prompt).contains("<pergunta> qual é a pergunta? </pergunta>");
    }

    @Test
    void promptSeparaMultiplosMatchesPorDuplaQuebraLinha() {
        // Arrange
        when(chatModel.chat(anyString())).thenReturn("resposta qualquer");
        List<EmbeddingMatch<TextSegment>> matches = List.of(
                matchWith("primeiro trecho"),
                matchWith("segundo trecho")
        );
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        chatService.ask("pergunta sobre os trechos", matches);

        // Assert
        verify(chatModel).chat(promptCaptor.capture());
        assertThat(promptCaptor.getValue())
                .contains("primeiro trecho\n\nsegundo trecho");
    }

    @Test
    void retornaRespostaDoChatModel() {
        // Arrange
        when(chatModel.chat(anyString())).thenReturn("resposta gerada pelo modelo");
        List<EmbeddingMatch<TextSegment>> matches = List.of(matchWith("contexto"));

        // Act
        ChatResponse response = chatService.ask("pergunta?", matches);

        // Assert
        assertThat(response.reply()).isEqualTo("resposta gerada pelo modelo");
    }
}
