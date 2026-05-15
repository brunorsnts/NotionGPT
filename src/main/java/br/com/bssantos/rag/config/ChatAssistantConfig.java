package br.com.bssantos.rag.config;

import br.com.bssantos.rag.memory.CaffeineChatMemoryStore;
import br.com.bssantos.rag.service.ChatAssistant;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatAssistantConfig {

    private final ChatModel chatModel;
    private final ChatMemoryProperties properties;
    private final CaffeineChatMemoryStore memoryStore;

    public ChatAssistantConfig(ChatModel chatModel,
                               ChatMemoryProperties properties,
                               CaffeineChatMemoryStore memoryStore) {
        this.chatModel = chatModel;
        this.properties = properties;
        this.memoryStore = memoryStore;
    }

    @Bean
    public ChatAssistant chatAssistant() {
        return AiServices.builder(ChatAssistant.class)
                .chatModel(chatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(properties.maxMessages())
                        .chatMemoryStore(memoryStore)
                        .build())
                .build();
    }
}
