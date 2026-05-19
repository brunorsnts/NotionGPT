package br.com.bssantos.rag.controller;

import br.com.bssantos.rag.dto.ChatRequest;
import br.com.bssantos.rag.handler.GlobalExceptionHandler;
import br.com.bssantos.rag.service.QueryService;
import br.com.bssantos.rag.service.QueryStreamService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ChatController.class, GlobalExceptionHandler.class})
class ChatControllerStreamTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private QueryService queryService;

    @MockitoBean
    private QueryStreamService queryStreamService;

    @Test
    void streamConsulta_retornaContentTypeEventStream() throws Exception {
        // Arrange
        String body = objectMapper.writeValueAsString(new ChatRequest("O que é RAG?"));
        doAnswer(inv -> {
            SseEmitter emitter = inv.getArgument(2);
            emitter.complete();
            return null;
        }).when(queryStreamService).askStreaming(any(), anyString(), any(SseEmitter.class));

        // Act
        MvcResult mvcResult = mockMvc.perform(post("/query/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(request().asyncStarted())
                .andReturn();

        // Assert
        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk());

        assertThat(mvcResult.getResponse().getContentType())
                .contains(MediaType.TEXT_EVENT_STREAM_VALUE);
    }

    @Test
    void streamConsulta_geraSessionId_quandoHeaderAusente() throws Exception {
        // Arrange
        String body = objectMapper.writeValueAsString(new ChatRequest("O que é RAG?"));

        // Act
        mockMvc.perform(post("/query/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(request().asyncStarted());

        // Assert — sessionId não nulo é gerado internamente e passado ao service
        verify(queryStreamService).askStreaming(any(ChatRequest.class), anyString(), any());
    }

    @Test
    void streamConsulta_usaSessionIdDoHeader_quandoPresente() throws Exception {
        // Arrange
        String sessionId = "minha-sessao-fixa";
        String body = objectMapper.writeValueAsString(new ChatRequest("O que é RAG?"));

        // Act
        mockMvc.perform(post("/query/stream")
                        .header("X-Session-Id", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(request().asyncStarted());

        // Assert
        verify(queryStreamService).askStreaming(any(ChatRequest.class), eq("minha-sessao-fixa"), any());
    }

    @Test
    void streamConsulta_retorna400_quandoQueryVazia() throws Exception {
        // Arrange
        String body = objectMapper.writeValueAsString(new ChatRequest(""));

        // Act & Assert
        mockMvc.perform(post("/query/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
