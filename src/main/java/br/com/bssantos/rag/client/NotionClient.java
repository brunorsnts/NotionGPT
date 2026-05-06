package br.com.bssantos.rag.client;

import br.com.bssantos.rag.dto.notion.NotionBlockListResponse;
import br.com.bssantos.rag.dto.notion.NotionBlockResponse;
import br.com.bssantos.rag.dto.notion.NotionPageListResponse;
import br.com.bssantos.rag.dto.notion.NotionPageResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.*;

@Component
public class NotionClient {

    private final RestClient restClient;
    private final String dataSourceId;

    public NotionClient(RestClient restClient, @Value("${notion-database-id}") String dataSourceId) {
        this.restClient = restClient;
        this.dataSourceId = dataSourceId;
    }

    public List<NotionPageResponse> buscarPaginas() {

        List<NotionPageResponse> list = new ArrayList<>();
        String cursor = null;
        NotionPageListResponse resposta;

        do {
            resposta = restClient
                    .post()
                    .uri("/data_sources/" + dataSourceId + "/query")
                    .body(cursor == null ? Map.of() : Map.of("start_cursor", cursor))
                    .retrieve().body(NotionPageListResponse.class);

            list.addAll(resposta.results());
            cursor = resposta.nextCursor();

        } while (resposta.hasMore());

        return list;
    }


    public List<NotionBlockResponse> buscarBlocos(String pageId) {
        List<NotionBlockResponse> list = new ArrayList<>();
        String cursor = null;
        NotionBlockListResponse resposta;

        do {
            String cursorAtual = cursor;
            resposta = restClient
                    .get()
                    .uri(builder -> builder
                            .path("/blocks/" + pageId + "/children")
                            .queryParamIfPresent("start_cursor", Optional.ofNullable(cursorAtual))
                            .build())
                    .retrieve().body(NotionBlockListResponse.class);
            list.addAll(resposta.results());
            cursor = resposta.nextCursor();
        } while (resposta.hasMore());

        return list;
    }
}
