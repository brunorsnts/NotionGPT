package br.com.bssantos.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties("chat.memory")
public record ChatMemoryProperties(int maxMessages, Duration ttl, int maxSessions) {
}
