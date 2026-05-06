package br.com.bssantos.rag.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient(RestClient.Builder builder, @Value("${notion-api-key}") String authorization) {
        return builder
                .baseUrl("https://api.notion.com/v1")
                .defaultHeaders(h -> {
                    h.add("Authorization", "Bearer " + authorization);
                    h.add("Notion-Version", "2026-03-11");
                })
                .build();
    }
}
