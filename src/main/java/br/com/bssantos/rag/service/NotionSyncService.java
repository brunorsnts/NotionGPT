package br.com.bssantos.rag.service;

import br.com.bssantos.rag.client.NotionClient;
import br.com.bssantos.rag.dto.notion.NotionPageResponse;
import br.com.bssantos.rag.util.NotionBlockExtractor;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class NotionSyncService {

    private static final Logger log = LoggerFactory.getLogger(NotionSyncService.class);

    private final NotionClient client;
    private final NotionBlockExtractor extractor;
    private final DocumentService documentService;

    public NotionSyncService(NotionClient client, NotionBlockExtractor extractor, DocumentService documentService) {
        this.client = client;
        this.extractor = extractor;
        this.documentService = documentService;
    }

    @Scheduled(cron = "0 0 0 * * *")
    public void sync() {
        log.info("Iniciando sync com o Notion...");
        var pages = client.buscarPaginas();
        log.info("Páginas encontradas: {}", pages.size());
        pages.forEach(this::processarPagina);
    }

    private void processarPagina(NotionPageResponse page) {
        var pageId = page.idAsUUID();
        var titulo = page.titulo();
        if (documentService.documentoJaAtualizado(pageId, page.lastEditedTimeAsInstant())) return;
        log.info("Processando página: {}", titulo);
        var content = extractor.extract(client.buscarBlocos(page.id()));
        var metadata = new Metadata()
                .put("pageId", pageId)
                .put("titulo", titulo)
                .put("lastEditedTime", page.lastEditedTime());
        documentService.salvaDocumento(Document.from(content, metadata));
        log.info("Página ingerida com sucesso: {}", titulo);
    }
}
