package br.com.bssantos.rag;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.bgesmallenv15q.BgeSmallEnV15QuantizedEmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        Document document = FileSystemDocumentLoader.loadDocument("C:/Users/Santo/Downloads/glossario.md");
        DocumentSplitter splitter = new DocumentByParagraphSplitter(1500, 200);
        List<TextSegment> textSegments = splitter.split(document);

        EmbeddingModel embeddingModel = new BgeSmallEnV15QuantizedEmbeddingModel();
        List<Embedding> embeddings = embeddingModel.embedAll(textSegments).content();

        EmbeddingStore<TextSegment> embeddingStore= new InMemoryEmbeddingStore<TextSegment>();

        List<String> ids = embeddingStore.addAll(embeddings, textSegments);
        Embedding embeddingQuery = embeddingModel
                .embed("O que é uma API REST").content();

        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(embeddingQuery)
                .maxResults(4)
                .build();

        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);

        searchResult.matches().forEach(System.out::println);
    }
}
