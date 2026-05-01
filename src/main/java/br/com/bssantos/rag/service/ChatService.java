package br.com.bssantos.rag.service;

import br.com.bssantos.rag.dto.ChatResponse;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private final ChatModel chatModel;

    public ChatService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    private String  buildPrompt(String question, List<EmbeddingMatch<TextSegment>> matches) {
        return ("""
                You are a helpful assistant that answers questions strictly based on the provided context.                                                                                                                \s
                  If the answer is not found in the context, say that you don't know.                                                                                                                                       \s
                  Do not use any external knowledge.
                
                  Context:
                  """ + matches.stream()
                .map(m -> m.embedded().text())
                .collect(Collectors.joining("\n\n"))
                + """
                
                  Question:
                  """ + question);
    }

    public ChatResponse ask(String question, List<EmbeddingMatch<TextSegment>> matches) {
        String prompt = buildPrompt(question, matches);
        return new ChatResponse(chatModel.chat(prompt));
    }
}
