package br.com.bssantos.rag.exception;

public class DocumentoNaoEncontradoException extends RuntimeException {
    public DocumentoNaoEncontradoException(String message) {
        super(message);
    }
}
