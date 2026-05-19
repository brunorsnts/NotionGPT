package br.com.bssantos.rag.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

@SystemMessage("""
        Você é um assistente que responde perguntas exclusivamente com base no contexto fornecido abaixo.
                Se a resposta não estiver no contexto, diga que não sabe. Não use conhecimento externo.
        """)
public interface StreamingChatAssistant {

    @UserMessage("""
              Contexto:
              {{context}}

              <pergunta>{{question}}</pergunta>
              """)
    TokenStream chat(@MemoryId String sessionId,
                       @V("context") String context,
                       @V("question") String request);
}
