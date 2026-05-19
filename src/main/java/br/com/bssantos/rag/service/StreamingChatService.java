package br.com.bssantos.rag.service;

import br.com.bssantos.rag.dto.ChatRequest;
import br.com.bssantos.rag.dto.RetrievalResult;
import dev.langchain4j.service.TokenStream;
import org.springframework.stereotype.Service;

@Service
public class StreamingChatService {

    private final StreamingChatAssistant streamingChatAssistant;

    public StreamingChatService(StreamingChatAssistant streamingChatAssistant) {
        this.streamingChatAssistant = streamingChatAssistant;
    }

    public TokenStream stream(ChatRequest request, RetrievalResult retrievalResult, String sessionId) {
        return streamingChatAssistant.chat(sessionId, retrievalResult.context(), request.query());
    }

}
