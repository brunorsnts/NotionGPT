package br.com.bssantos.rag.config;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatModelAntrhopic {

    @Value("${anthropic-api-key}")
    private String apiKey;

    @Bean
    public ChatModel chatModel() {
        return AnthropicChatModel.builder()
                .apiKey(apiKey)
                .modelName("claude-haiku-4-5-20251001")
                .maxTokens(512)
                .cacheSystemMessages(true)
                .cacheTools(true)
                .topK(30)
                .logRequests(true)
                .logResponses(true)
                .build();
    }
}
