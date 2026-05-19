package br.com.bssantos.rag.service;

import br.com.bssantos.rag.dto.ChatResponse;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final ChatAssistant chatAssistant;

    public ChatService(ChatAssistant chatAssistant) {
        this.chatAssistant = chatAssistant;
    }

    public ChatResponse ask(String question, String context, String sessionId) {
        return new ChatResponse(chatAssistant.chat(sessionId, context, question));
    }
}
