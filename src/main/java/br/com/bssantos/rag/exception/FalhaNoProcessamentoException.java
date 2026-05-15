package br.com.bssantos.rag.exception;

public class FalhaNoProcessamentoException extends RuntimeException {
    public FalhaNoProcessamentoException(String message) {
        super(message);
    }

    public FalhaNoProcessamentoException(String message, Throwable cause) {
        super(message, cause);
    }
}
