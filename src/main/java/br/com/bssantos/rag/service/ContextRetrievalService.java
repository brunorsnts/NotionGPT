package br.com.bssantos.rag.service;

import br.com.bssantos.rag.dto.ChatRequest;
import br.com.bssantos.rag.dto.RetrievalResult;
import br.com.bssantos.rag.exception.FalhaNoProcessamentoException;
import br.com.bssantos.rag.observability.FailureStage;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ContextRetrievalService {

    private static final Logger log = LoggerFactory.getLogger(ContextRetrievalService.class);

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;


    public ContextRetrievalService(@Qualifier("embeddingQuery") EmbeddingModel embeddingModel,
                                   EmbeddingStore<TextSegment> embeddingStore) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
    }

    @Transactional(readOnly = true)
    public RetrievalResult buildContext(ChatRequest chatRequest) {
        Embedding embedding = embed(chatRequest);
        EmbeddingSearchRequest searchRequest = searchRequest(embedding);
        List<EmbeddingMatch<TextSegment>> matches = result(searchRequest);

        return buildRetrievalResult(matches);
    }

    private Embedding embed(ChatRequest request) {
        try {
            return embeddingModel.embed(request.query()).content();
        } catch (RuntimeException ex) {
            throw new FalhaNoProcessamentoException("Estamos enfrentando problemas com a API da CohereClient. Tente novamente mais tarde", FailureStage.EMBED);
        }
    }

    private EmbeddingSearchRequest searchRequest(Embedding embedding) {
        return EmbeddingSearchRequest.builder()
                .queryEmbedding(embedding)
                .maxResults(6)
                .minScore(0.5)
                .build();

    }

    private List<EmbeddingMatch<TextSegment>> result(EmbeddingSearchRequest searchRequest) {
        try {
            EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
            return searchResult.matches();
        } catch (RuntimeException ex) {
            throw new FalhaNoProcessamentoException("Estamos enfrentando problema com a conexão com o banco de dados", FailureStage.SEARCH);
        }
    }

    private RetrievalResult buildRetrievalResult(List<EmbeddingMatch<TextSegment>> matches) {

        if (matches.isEmpty()) {
            throw new FalhaNoProcessamentoException("Nenhum conteúdo relevante encontrado nas suas anotações para responder essa pergunta", FailureStage.EMPTY);
        }

        int matchesCount = matches.size();
        List<Double> scores = matches.stream().map(EmbeddingMatch::score).toList();
        List<String> titulos = matches.stream()
                .map(m -> m.embedded().metadata().getString("titulo")).toList();

        String contexto = matches.stream()
                .map(m -> m.embedded().text())
                .collect(Collectors.joining("\n\n"));
        return new RetrievalResult(contexto, matchesCount, scores, titulos);
    }
}
