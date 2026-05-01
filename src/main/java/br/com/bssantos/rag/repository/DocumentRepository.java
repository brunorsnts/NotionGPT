package br.com.bssantos.rag.repository;

import br.com.bssantos.rag.entity.StudyDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentRepository extends JpaRepository<StudyDocument, UUID> {
}
