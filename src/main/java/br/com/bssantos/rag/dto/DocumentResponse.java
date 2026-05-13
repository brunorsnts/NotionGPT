package br.com.bssantos.rag.dto;

import br.com.bssantos.rag.entity.StudyDocument;

import java.time.Instant;
import java.util.UUID;

public record DocumentResponse (
    UUID id,
    String name,
    Instant addedAt
) {
    public DocumentResponse(StudyDocument document) {
        this(document.getId(), document.getNome(), document.getData());
    }
}
