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

    private String buildPrompt(String question, List<EmbeddingMatch<TextSegment>> matches) {
        String context = matches.stream()
                .map(m -> m.embedded().text())
                .collect(Collectors.joining("\n\n"));
        return """
                Você é um assistente que responde perguntas exclusivamente com base no contexto fornecido abaixo.
                Se a resposta não estiver no contexto, diga que não sabe. Não use conhecimento externo.

                Contexto:
                    %s

                Pergunta:
                    <pergunta> %s </pergunta>""".formatted(context, question);
    }

    public ChatResponse ask(String question, List<EmbeddingMatch<TextSegment>> matches) {
        String prompt = buildPrompt(question, matches);
        return new ChatResponse(chatModel.chat(prompt));
    }
}
