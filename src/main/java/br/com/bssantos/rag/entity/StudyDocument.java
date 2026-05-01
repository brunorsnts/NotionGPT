package br.com.bssantos.rag.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents")
public class StudyDocument {

    @Id
    private UUID id;
    private String nome;
    private String arquivo;
    private Instant data;

    public StudyDocument() {
    }

    public StudyDocument(UUID id, String nome, String arquivo, Instant data) {
        this.id = id;
        this.nome = nome;
        this.arquivo = arquivo;
        this.data = data;
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getArquivo() {
        return arquivo;
    }

    public Instant getData() {
        return data;
    }
}
