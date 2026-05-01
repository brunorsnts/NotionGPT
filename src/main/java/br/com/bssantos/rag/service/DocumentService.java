package br.com.bssantos.rag.service;

import br.com.bssantos.rag.dto.DocumentResponse;
import br.com.bssantos.rag.entity.StudyDocument;
import br.com.bssantos.rag.repository.DocumentRepository;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentByParagraphSplitter;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DocumentService {

    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;
    private final DocumentRepository documentRepository;
    private final DocumentSplitter splitter = new DocumentByParagraphSplitter(1500, 200);
    private final DocumentParser parser = new ApacheTikaDocumentParser();

    public DocumentService(@Qualifier("embeddingDocument") EmbeddingModel embeddingModel,
                           EmbeddingStore<TextSegment> embeddingStore,
                           DocumentRepository documentRepository) {
        this.embeddingModel = embeddingModel;
        this.embeddingStore = embeddingStore;
        this.documentRepository = documentRepository;
    }

    public void deletaDocumento(String id) {
        try {
            UUID uuid = UUID.fromString(id);
            documentRepository.deleteById(uuid);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("O Id informado é inválido");
        }
    }

    public Page<DocumentResponse> retornaTodosOsDocumentos(Pageable pageable) {
        return documentRepository.findAll(pageable)
                .map(DocumentResponse::new);
    }

    public DocumentResponse salvaDocumento(MultipartFile file) {
        UUID uuid = UUID.randomUUID();
        String fileName = extraFileName(file, uuid);
        Document document = extraiConteudoDoArquivo(file);

        List<TextSegment> textSegments = geraOsTextSegments(document, uuid);

        List<Embedding> embeddings = embeddingModel.embedAll(textSegments).content();
        embeddingStore.addAll(embeddings, textSegments);
        StudyDocument studyDocument = new StudyDocument(uuid, fileName, fileName, Instant.now());
        documentRepository.save(studyDocument);

        return new DocumentResponse(uuid, fileName,"//", Instant.now());
    }

    private String extraFileName(MultipartFile file, UUID uuid) {
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            fileName = "documento_" + uuid.toString().substring(0, 8) + ".pdf";
        } else {
            fileName = new java.io.File(fileName).getName();
        }
        return fileName;
    }

    private Document extraiConteudoDoArquivo(MultipartFile file) {
        try {
            return parser.parse(file.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<TextSegment> geraOsTextSegments(Document document, UUID uuid) {
        return splitter.split(document)
                .stream()
                .map(ts -> {
                    ts.metadata().put("document_id", uuid.toString());
                    return ts;
                })
                .toList();
    }
}
