package br.com.bssantos.rag.exception;

public class DocumentoInvalidoException extends RuntimeException {
    public DocumentoInvalidoException(String message) {
        super(message);
    }
}
