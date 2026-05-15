package br.com.bssantos.rag.controller;

import br.com.bssantos.rag.dto.ChatRequest;
import br.com.bssantos.rag.dto.ChatResponse;
import br.com.bssantos.rag.exception.FalhaNoProcessamentoException;
import br.com.bssantos.rag.handler.GlobalExceptionHandler;
import br.com.bssantos.rag.service.QueryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ChatController.class, GlobalExceptionHandler.class})
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private QueryService queryService;

    @Test
    void queryEmBrancoRetorna400() throws Exception {
        // Arrange
        String body = objectMapper.writeValueAsString(new ChatRequest(""));

        // Act & Assert
        mockMvc.perform(post("/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void queryNulaRetorna400() throws Exception {
        // Arrange
        String body = "{\"query\": null}";

        // Act & Assert
        mockMvc.perform(post("/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void queryComMaisDe1000CaracteresRetorna400() throws Exception {
        // Arrange
        String queryLonga = "a".repeat(1001);
        String body = objectMapper.writeValueAsString(new ChatRequest(queryLonga));

        // Act & Assert
        mockMvc.perform(post("/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void queryComExatamente1000CaracteresRetorna200() throws Exception {
        // Arrange
        String queryNoLimite = "a".repeat(1000);
        when(queryService.askIA(any(ChatRequest.class)))
                .thenReturn(new ChatResponse("resposta válida"));
        String body = objectMapper.writeValueAsString(new ChatRequest(queryNoLimite));

        // Act & Assert
        mockMvc.perform(post("/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("resposta válida"));
    }

    @Test
    void queryValidaRetorna200ComResposta() throws Exception {
        // Arrange
        when(queryService.askIA(any(ChatRequest.class)))
                .thenReturn(new ChatResponse("resposta gerada"));
        String body = objectMapper.writeValueAsString(new ChatRequest("O que é RAG?"));

        // Act & Assert
        mockMvc.perform(post("/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("resposta gerada"));
    }

    @Test
    void falhaNoProcessamentoRetorna500() throws Exception {
        // Arrange
        when(queryService.askIA(any(ChatRequest.class)))
                .thenThrow(new FalhaNoProcessamentoException("Nenhum conteúdo relevante encontrado nas suas anotações para responder essa pergunta"));
        String body = objectMapper.writeValueAsString(new ChatRequest("pergunta sem contexto"));

        // Act & Assert
        mockMvc.perform(post("/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isInternalServerError());
    }
}
