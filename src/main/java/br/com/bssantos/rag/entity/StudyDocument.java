package br.com.bssantos.rag.entity;

import dev.langchain4j.data.document.Document;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class StudyDocument {

    @Id
    private UUID id;
    private String nome;
    private Instant data;
    private Instant lastEditedTime;

    public StudyDocument() {
    }

    public StudyDocument(UUID id, Document document, Instant data) {
        this.id = id;
        this.nome = document.metadata().getString("titulo");
        this.data = data;
        this.lastEditedTime = Instant.parse(document.metadata().getString("lastEditedTime"));
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public Instant getData() {
        return data;
    }

    public Instant getLastEditedTime() {
        return lastEditedTime;
    }
}
