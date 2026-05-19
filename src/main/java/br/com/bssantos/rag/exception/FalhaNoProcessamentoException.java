package br.com.bssantos.rag.exception;

import br.com.bssantos.rag.observability.FailureStage;

public class FalhaNoProcessamentoException extends RuntimeException {

    private final FailureStage failureStage;

    public FalhaNoProcessamentoException(String message) {
        super(message);
        this.failureStage = FailureStage.NONE;
    }

    public FalhaNoProcessamentoException(String message, Throwable cause) {
        super(message, cause);
        this.failureStage = FailureStage.NONE;
    }

    public FalhaNoProcessamentoException(String message, FailureStage failureStage) {
        super(message);
        this.failureStage = failureStage;
    }

    public FailureStage getFailureStage() {
        return failureStage;
    }
}
