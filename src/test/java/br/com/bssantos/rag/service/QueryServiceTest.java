package br.com.bssantos.rag.service;

import br.com.bssantos.rag.dto.ChatRequest;
import br.com.bssantos.rag.dto.ChatResponse;
import br.com.bssantos.rag.exception.FalhaNoProcessamentoException;
import br.com.bssantos.rag.observability.QueryMetricsService;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QueryServiceTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @Mock
    private ChatService chatService;

    @Mock
    private EmbeddingStore<TextSegment> embeddingStore;

    @Mock
    private QueryMetricsService queryMetricsService;

    private QueryService queryService;

    private static final Embedding DUMMY_EMBEDDING = Embedding.from(new float[]{0.1f, 0.2f, 0.3f});

    @BeforeEach
    void setUp() {
        queryService = new QueryService(embeddingModel, chatService, embeddingStore, queryMetricsService);
    }

    private EmbeddingMatch<TextSegment> buildMatch(String text) {
        return new EmbeddingMatch<>(0.8, "id-1", DUMMY_EMBEDDING, TextSegment.from(text));
    }

    @Test
    void lancaFalhaNoProcessamentoQuandoResultadoEhVazio() {
        // Arrange
        when(embeddingModel.embed(any(String.class))).thenReturn(Response.from(DUMMY_EMBEDDING));
        when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
                .thenReturn(new EmbeddingSearchResult<>(List.of()));

        // Act & Assert
        assertThatThrownBy(() -> queryService.askIA(new ChatRequest("o que é LangChain4J?")))
                .isInstanceOf(FalhaNoProcessamentoException.class)
                .hasMessage("Nenhum conteúdo relevante encontrado nas suas anotações para responder essa pergunta");
    }

    @Test
    void delegaParaChatServiceQuandoHaResultados() {
        // Arrange
        List<EmbeddingMatch<TextSegment>> matches = List.of(buildMatch("conteudo relevante"));
        when(embeddingModel.embed(any(String.class))).thenReturn(Response.from(DUMMY_EMBEDDING));
        when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
                .thenReturn(new EmbeddingSearchResult<>(matches));
        when(chatService.ask(any(String.class), any())).thenReturn(new ChatResponse("resposta"));

        // Act
        ChatResponse response = queryService.askIA(new ChatRequest("minha pergunta"));

        // Assert
        verify(chatService).ask("minha pergunta", matches);
        assertThat(response.reply()).isEqualTo("resposta");
    }

    @Test
    void searchRequestUsaMaxResultsSeisMinimoScoreMeioPonto() {
        // Arrange
        List<EmbeddingMatch<TextSegment>> matches = List.of(buildMatch("algum texto"));
        when(embeddingModel.embed(any(String.class))).thenReturn(Response.from(DUMMY_EMBEDDING));
        when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
                .thenReturn(new EmbeddingSearchResult<>(matches));
        when(chatService.ask(any(String.class), any())).thenReturn(new ChatResponse("resposta"));
        ArgumentCaptor<EmbeddingSearchRequest> searchCaptor =
                ArgumentCaptor.forClass(EmbeddingSearchRequest.class);

        // Act
        queryService.askIA(new ChatRequest("pergunta de teste"));

        // Assert
        verify(embeddingStore).search(searchCaptor.capture());
        EmbeddingSearchRequest captured = searchCaptor.getValue();
        assertThat(captured.maxResults()).isEqualTo(6);
        assertThat(captured.minScore()).isEqualTo(0.5);
    }

    @Test
    void lancaFalhaNoProcessamentoQuandoChatServiceLancaRuntimeException() {
        // Arrange
        List<EmbeddingMatch<TextSegment>> matches = List.of(buildMatch("contexto"));
        when(embeddingModel.embed(any(String.class))).thenReturn(Response.from(DUMMY_EMBEDDING));
        when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
                .thenReturn(new EmbeddingSearchResult<>(matches));
        when(chatService.ask(any(String.class), any()))
                .thenThrow(new RuntimeException("falha de rede"));

        // Act & Assert
        assertThatThrownBy(() -> queryService.askIA(new ChatRequest("pergunta")))
                .isInstanceOf(FalhaNoProcessamentoException.class)
                .hasMessage("Houve um problema na comunicação com a API da LLM");
    }

    @Test
    void lancaFalhaNoProcessamentoQuandoEmbeddingStoreLancaRuntimeException() {
        // Arrange
        when(embeddingModel.embed(any(String.class))).thenReturn(Response.from(DUMMY_EMBEDDING));
        when(embeddingStore.search(any(EmbeddingSearchRequest.class)))
                .thenThrow(new RuntimeException("conexão com banco falhou"));

        // Act & Assert
        assertThatThrownBy(() -> queryService.askIA(new ChatRequest("pergunta")))
                .isInstanceOf(FalhaNoProcessamentoException.class)
                .hasMessage("Estamos enfrentando problema com a conexão com o banco de dados");
    }
}
