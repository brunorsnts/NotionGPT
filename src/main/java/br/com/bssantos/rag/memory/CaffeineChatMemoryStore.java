package br.com.bssantos.rag.memory;

import br.com.bssantos.rag.config.ChatMemoryProperties;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class CaffeineChatMemoryStore implements ChatMemoryStore {

    private final Cache<Object, List<ChatMessage>> cache;

    public CaffeineChatMemoryStore(ChatMemoryProperties chatMemoryProperties) {
        this.cache = Caffeine.newBuilder()
                .expireAfterAccess(chatMemoryProperties.ttl())
                .maximumSize(chatMemoryProperties.maxSessions())
                .build();
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        List<ChatMessage> messages = cache.getIfPresent(memoryId);
        return messages != null ? messages : new ArrayList<>();
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        cache.put(memoryId, messages);
    }

    @Override
    public void deleteMessages(Object memoryId) {
        cache.invalidate(memoryId);
    }
}
