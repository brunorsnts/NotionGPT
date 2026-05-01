package br.com.bssantos.rag.service;

import br.com.bssantos.rag.dto.DocumentResponse;
import br.com.bssantos.rag.entity.StudyDocument;
import br.com.bssantos.rag.exception.DocumentoInvalidoException;
import br.com.bssantos.rag.exception.DocumentoNaoEncontradoException;
import br.com.bssantos.rag.exception.FalhaNoProcessamentoException;
import br.com.bssantos.rag.exception.IdInvalidoException;
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
import java.nio.file.Path;
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
            StudyDocument studyDocument = documentRepository.findById(uuid).orElseThrow(
                    () -> new DocumentoNaoEncontradoException("Nenhum documento foi encontrado para o ID especificado"));
            documentRepository.delete(studyDocument);
        } catch (IllegalArgumentException ex) {
            throw new IdInvalidoException("O Id informado é inválido");
        }
    }

    public Page<DocumentResponse> retornaTodosOsDocumentos(Pageable pageable) {
        return documentRepository.findAll(pageable)
                .map(DocumentResponse::new);
    }

    public DocumentResponse salvaDocumento(MultipartFile file) {
        if (file.isEmpty()) {
            throw new DocumentoInvalidoException("O arquivo enviado está vazio");
        }

        UUID uuid = UUID.randomUUID();
        Document document;
        String fileName = extraFileName(file, uuid);
        try {
            document = extraiConteudoDoArquivo(file);
        } catch (IOException ex) {
            throw new DocumentoInvalidoException("O documento fornecido é inválido ou está corrompido");
        }

        List<TextSegment> textSegments = geraOsTextSegments(document, uuid);

        List<Embedding> embeddings;
        try {
            embeddings = embeddingModel.embedAll(textSegments).content();
        } catch (RuntimeException ex) {
            throw new FalhaNoProcessamentoException("Estamos enfrentando problemas com a API da CohereClient. Tente novamente mais tarde");
        }

        try {
            embeddingStore.addAll(embeddings, textSegments);
        } catch (RuntimeException ex) {
            throw new FalhaNoProcessamentoException("Estamos enfrentando problema com a conexão com o banco de dados");
        }

        StudyDocument studyDocument = new StudyDocument(uuid, fileName, fileName, Instant.now());
        documentRepository.save(studyDocument);
        return new DocumentResponse(studyDocument);

    }

    private String extraFileName(MultipartFile file, UUID uuid) {
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isBlank()) {
            fileName = "documento_" + uuid.toString().substring(0, 8) + ".pdf";
        } else {
            fileName = Path.of(fileName).getFileName().toString();
        }
        return fileName;
    }

    private Document extraiConteudoDoArquivo(MultipartFile file) throws IOException {
        return parser.parse(file.getInputStream());
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
